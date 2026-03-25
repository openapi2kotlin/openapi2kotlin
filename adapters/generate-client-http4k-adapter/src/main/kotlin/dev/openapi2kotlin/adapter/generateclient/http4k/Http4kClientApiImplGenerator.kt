package dev.openapi2kotlin.adapter.generateclient.http4k

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.UNIT
import dev.openapi2kotlin.application.core.openapi2kotlin.model.api.ApiDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.api.ApiEndpointDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.api.ApiParamDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.ListTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.ModelDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.TrivialTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.raw.RawPathDO
import dev.openapi2kotlin.application.core.openapi2kotlin.port.GenerateApiPort
import dev.openapi2kotlin.tools.generatortools.TypeNameContext
import dev.openapi2kotlin.tools.generatortools.postProcess
import dev.openapi2kotlin.tools.generatortools.toTypeName

private val HTTP_HANDLER_T = ClassName("org.http4k.core", "HttpHandler")
private val REQUEST_T = ClassName("org.http4k.core", "Request")
private val METHOD_T = ClassName("org.http4k.core", "Method")
private val BODY_T = ClassName("org.http4k.core", "Body")

internal class Http4kClientApiImplGenerator : GenerateApiPort {
    override fun generateApi(command: GenerateApiPort.Command) {
        val outDir = command.outputDirPath.toFile()
        val bySchemaName: Map<String, ModelDO> = command.models.associateBy { it.rawSchema.originalName }
        val ctx =
            TypeNameContext(
                modelPackageName = command.modelPackageName,
                bySchemaName = bySchemaName,
            )

        command.apiContext.apis.forEach { api ->
            FileSpec.builder(command.packageName, "${api.generatedName}Impl")
                .indent("    ")
                .addImport("org.http4k.format.KotlinxSerialization", "auto")
                .addType(generateApiImpl(api, command.packageName, command.apiContext.basePath, ctx))
                .build()
                .writeTo(outDir)
        }

        outDir.postProcess()
    }

    private fun generateApiImpl(
        api: ApiDO,
        apiPackageName: String,
        basePath: String,
        ctx: TypeNameContext,
    ): TypeSpec {
        val apiType = ClassName(apiPackageName, api.generatedName)

        return TypeSpec.classBuilder("${api.generatedName}Impl")
            .addSuperinterface(apiType)
            .primaryConstructor(
                FunSpec.constructorBuilder()
                    .addParameter("client", HTTP_HANDLER_T)
                    .build(),
            )
            .addProperty(
                PropertySpec.builder("client", HTTP_HANDLER_T, KModifier.PRIVATE)
                    .initializer("client")
                    .build(),
            )
            .addProperty(
                PropertySpec.builder("basePath", String::class, KModifier.PRIVATE)
                    .initializer("%S", basePath)
                    .build(),
            )
            .addFunction(
                FunSpec.builder("resolvePath")
                    .addModifiers(KModifier.PRIVATE)
                    .addParameter("apiPath", String::class)
                    .returns(String::class)
                    .addCode(
                        """
                        val normalizedBasePath = basePath.trim()
                        val normalizedApiPath = apiPath.trim()

                        return when {
                          normalizedBasePath.isBlank() && normalizedApiPath.isBlank() -> ""
                          normalizedBasePath.isBlank() -> if (normalizedApiPath.startsWith("/")) normalizedApiPath else "/${'$'}normalizedApiPath"
                          normalizedApiPath.isBlank() -> if (normalizedBasePath.startsWith("/")) normalizedBasePath else "/${'$'}normalizedBasePath"
                          else -> {
                            val base = normalizedBasePath.trimEnd('/')
                            val path = normalizedApiPath.trimStart('/')
                            if (base.startsWith("/")) "${'$'}base/${'$'}path" else "/${'$'}base/${'$'}path"
                          }
                        }
                        """.trimIndent(),
                    )
                    .build(),
            )
            .apply {
                api.endpoints.forEach { endpoint ->
                    addFunction(generateRawMethod(endpoint, ctx))
                    addFunction(generateBodyMethod(endpoint, ctx))
                }
            }
            .build()
    }

    private fun generateBodyMethod(
        ep: ApiEndpointDO,
        ctx: TypeNameContext,
    ): FunSpec {
        val returnType = Http4kClientApiPolicy.bodyReturnType(ep, ctx)
        val builder =
            overrideFunBuilder(ep, ctx)
                .returns(returnType)

        when {
            returnType == UNIT -> builder.addCode("%L\n", rawCall(ep))
            (ep.successResponse?.type as? TrivialTypeDO)?.kind == TrivialTypeDO.Kind.BYTE_ARRAY ->
                builder.addCode("return %L.bodyString().encodeToByteArray()\n", rawCall(ep))
            else ->
                builder.addCode(
                    """
                    val response = %L
                    val lens = %T.auto<%T>().toLens()
                    return lens(response)
                    """.trimIndent() + "\n",
                    rawCall(ep),
                    BODY_T,
                    returnType,
                )
        }

        return builder.build()
    }

    private fun generateRawMethod(
        ep: ApiEndpointDO,
        ctx: TypeNameContext,
    ): FunSpec =
        overrideFunBuilder(ep, ctx, suffix = "WithHttpInfo")
            .returns(Http4kClientApiPolicy.httpInfoReturnType())
            .addCode(buildRawMethodCode(ep, ctx))
            .build()

    private fun overrideFunBuilder(
        ep: ApiEndpointDO,
        ctx: TypeNameContext,
        suffix: String = "",
    ): FunSpec.Builder {
        val builder =
            FunSpec.builder(ep.generatedName + suffix)
                .addModifiers(KModifier.OVERRIDE)

        ep.params.forEach { param ->
            builder.addParameter(
                ParameterSpec.builder(param.generatedName, param.type.toTypeName(ctx)).build(),
            )
        }

        ep.requestBody?.let { body ->
            builder.addParameter(
                ParameterSpec.builder(body.generatedName, body.type.toTypeName(ctx)).build(),
            )
        }

        val kdoc =
            buildString {
                ep.rawOperation.summary?.let { appendLine(it) }
                ep.rawOperation.description?.let {
                    if (isNotEmpty()) appendLine()
                    appendLine(it)
                }
            }
        if (kdoc.isNotBlank()) builder.addKdoc(kdoc)

        return builder
    }

    private fun rawCall(ep: ApiEndpointDO): String =
        buildString {
            append(ep.generatedName)
            append("WithHttpInfo(")
            append(
                buildList {
                    ep.params.forEach { add(it.generatedName) }
                    ep.requestBody?.let { add(it.generatedName) }
                }.joinToString(", "),
            )
            append(")")
        }

    private fun buildRawMethodCode(
        ep: ApiEndpointDO,
        ctx: TypeNameContext,
    ): CodeBlock {
        val builder = CodeBlock.builder()
        builder.addStatement("var resolvedPath = resolvePath(%S)", ep.rawOperation.path)
        builder.addPathParams(ep)
        builder.addStatement(
            "var request = %T(%T.%L, resolvedPath)",
            REQUEST_T,
            METHOD_T,
            ep.rawOperation.httpMethod.http4kMethodName(),
        )
        builder.addQueryParams(ep)
        builder.addHeaderParams(ep)
        builder.addRequestBody(ep, ctx)

        builder.addStatement("return client(request)")
        return builder.build()
    }
}

private fun RawPathDO.HttpMethodDO.http4kMethodName(): String =
    when (this) {
        RawPathDO.HttpMethodDO.GET -> "GET"
        RawPathDO.HttpMethodDO.POST -> "POST"
        RawPathDO.HttpMethodDO.PUT -> "PUT"
        RawPathDO.HttpMethodDO.PATCH -> "PATCH"
        RawPathDO.HttpMethodDO.DELETE -> "DELETE"
    }

private fun CodeBlock.Builder.addPathParams(ep: ApiEndpointDO) {
    ep.params
        .filter { it.rawParam.location == RawPathDO.ParamLocationDO.PATH }
        .forEach { param ->
            val pathValue =
                if ((param.type as? TrivialTypeDO)?.kind == TrivialTypeDO.Kind.STRING) {
                    CodeBlock.of("%L", param.generatedName)
                } else {
                    CodeBlock.of("%L.toString()", param.generatedName)
                }
            addStatement(
                "resolvedPath = resolvedPath.replace(%S, %L)",
                "{${param.rawParam.name}}",
                pathValue,
            )
        }
}

private fun CodeBlock.Builder.addQueryParams(ep: ApiEndpointDO) {
    ep.params
        .filter { it.rawParam.location == RawPathDO.ParamLocationDO.QUERY }
        .forEach { param ->
            when (param.type) {
                is ListTypeDO -> addListQueryParam(param)
                else -> addScalarQueryParam(param)
            }
        }
}

private fun CodeBlock.Builder.addHeaderParams(ep: ApiEndpointDO) {
    ep.params
        .filter { it.rawParam.location == RawPathDO.ParamLocationDO.HEADER }
        .forEach(::addHeaderParam)
}

private fun CodeBlock.Builder.addRequestBody(
    ep: ApiEndpointDO,
    ctx: TypeNameContext,
) {
    val body = ep.requestBody ?: return
    val bodyType = body.type.toTypeName(ctx)
    val isByteArray = (body.type as? TrivialTypeDO)?.kind == TrivialTypeDO.Kind.BYTE_ARRAY

    when {
        isByteArray && bodyType.isNullable ->
            addStatement(
                "%L?.let { request = request.body(it.decodeToString()) }",
                body.generatedName,
            )

        isByteArray ->
            addStatement(
                "request = request.body(%L.decodeToString())",
                body.generatedName,
            )

        bodyType.isNullable ->
            addStatement(
                """
                %1L?.let {
                    val lens = %2T.auto<%3T>().toLens()
                    request = lens(it, request)
                }
                """.trimIndent(),
                body.generatedName,
                BODY_T,
                bodyType.copy(nullable = false),
            )

        else ->
            addStatement(
                """
                run {
                    val lens = %1T.auto<%2T>().toLens()
                    request = lens(%3L, request)
                }
                """.trimIndent(),
                BODY_T,
                bodyType,
                body.generatedName,
            )
    }
}

private fun CodeBlock.Builder.addListQueryParam(param: ApiParamDO) {
    val listType = param.type as ListTypeDO
    val elementKind = (listType.elementType as? TrivialTypeDO)?.kind
    val itemValue = if (elementKind == TrivialTypeDO.Kind.STRING) "it" else "it.toString()"

    if (param.rawParam.required) {
        addStatement(
            "%L.forEach { request = request.query(%S, $itemValue) }",
            param.generatedName,
            param.rawParam.name,
        )
    } else {
        addStatement(
            "%L?.forEach { request = request.query(%S, $itemValue) }",
            param.generatedName,
            param.rawParam.name,
        )
    }
}

private fun CodeBlock.Builder.addScalarQueryParam(param: ApiParamDO) {
    val isString = (param.type as? TrivialTypeDO)?.kind == TrivialTypeDO.Kind.STRING

    if (param.rawParam.required) {
        if (isString) {
            addStatement(
                "request = request.query(%S, %L)",
                param.rawParam.name,
                param.generatedName,
            )
        } else {
            addStatement(
                "request = request.query(%S, %L.toString())",
                param.rawParam.name,
                param.generatedName,
            )
        }
    } else if (isString) {
        addStatement(
            "%L?.let { request = request.query(%S, it) }",
            param.generatedName,
            param.rawParam.name,
        )
    } else {
        addStatement(
            "%L?.let { request = request.query(%S, it.toString()) }",
            param.generatedName,
            param.rawParam.name,
        )
    }
}

private fun CodeBlock.Builder.addHeaderParam(param: ApiParamDO) {
    val isString = (param.type as? TrivialTypeDO)?.kind == TrivialTypeDO.Kind.STRING

    if (param.rawParam.required) {
        if (isString) {
            addStatement(
                "request = request.header(%S, %L)",
                param.rawParam.name,
                param.generatedName,
            )
        } else {
            addStatement(
                "request = request.header(%S, %L.toString())",
                param.rawParam.name,
                param.generatedName,
            )
        }
    } else if (isString) {
        addStatement(
            "%L?.let { request = request.header(%S, it) }",
            param.generatedName,
            param.rawParam.name,
        )
    } else {
        addStatement(
            "%L?.let { request = request.header(%S, it.toString()) }",
            param.generatedName,
            param.rawParam.name,
        )
    }
}
