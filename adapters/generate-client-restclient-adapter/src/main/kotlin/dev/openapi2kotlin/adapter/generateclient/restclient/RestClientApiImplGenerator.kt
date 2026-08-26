package dev.openapi2kotlin.adapter.generateclient.restclient

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.api.ApiDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.api.ApiEndpointDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.api.ApiParamDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.model.ListTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.model.ModelDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.model.TrivialTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.raw.RawPathDO
import dev.openapi2kotlin.application.core.openapi2kotlin.port.GenerateApiPort
import dev.openapi2kotlin.tools.generatortools.TypeNameContext
import dev.openapi2kotlin.tools.generatortools.postProcess
import dev.openapi2kotlin.tools.generatortools.toTypeName

private val REST_CLIENT_T = ClassName("org.springframework.web.client", "RestClient")
private val RESPONSE_SPEC_T = ClassName("org.springframework.web.client.RestClient", "ResponseSpec")
private val HTTP_METHOD_T = ClassName("org.springframework.http", "HttpMethod")
private val PARAMETERIZED_TYPE_REFERENCE_T =
    ClassName("org.springframework.core", "ParameterizedTypeReference")

private data class ClientGenerationContext(
    val apiPackageName: String,
    val basePath: String,
    val typeNames: TypeNameContext,
    val encodePathParameterSlashes: Boolean,
)

internal class RestClientApiImplGenerator : GenerateApiPort {
    override fun generateApi(command: GenerateApiPort.Command) {
        val outDir = command.outputDirPath.toFile()
        val bySchemaName: Map<String, ModelDO> = command.models.associateBy { it.rawSchema.originalName }
        val ctx =
            ClientGenerationContext(
                apiPackageName = command.packageName,
                basePath = command.apiContext.basePath,
                typeNames =
                    TypeNameContext(
                        modelPackageName = command.modelPackageName,
                        bySchemaName = bySchemaName,
                    ),
                encodePathParameterSlashes = command.encodePathParameterSlashes,
            )

        command.apiContext.apis.forEach { api ->
            FileSpec
                .builder(command.packageName, "${api.generatedName}Impl")
                .indent("    ")
                .addType(
                    generateApiImpl(
                        api = api,
                        ctx = ctx,
                    ),
                )
                .build()
                .writeTo(outDir)
        }

        outDir.postProcess()
    }

    private fun generateApiImpl(
        api: ApiDO,
        ctx: ClientGenerationContext,
    ): TypeSpec {
        val apiType = ClassName(ctx.apiPackageName, api.generatedName)

        return TypeSpec
            .classBuilder("${api.generatedName}Impl")
            .addSuperinterface(apiType)
            .primaryConstructor(
                FunSpec
                    .constructorBuilder()
                    .addParameter("restClient", REST_CLIENT_T)
                    .build(),
            ).addProperty(
                PropertySpec
                    .builder("restClient", REST_CLIENT_T, KModifier.PRIVATE)
                    .initializer("restClient")
                    .build(),
            ).addProperty(
                PropertySpec
                    .builder("basePath", String::class, KModifier.PRIVATE)
                    .initializer("%S", ctx.basePath)
                    .build(),
            ).addFunction(
                FunSpec
                    .builder("resolvePath")
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
                    ).build(),
            ).apply {
                api.endpoints.forEach { endpoint ->
                    addFunction(generateRequestSpecFun(endpoint, ctx))
                    addFunction(generateBodyMethod(endpoint, ctx.typeNames))
                    addFunction(generateHttpInfoMethod(endpoint, ctx.typeNames))
                    addFunction(generateResponseSpecMethod(endpoint, ctx.typeNames))
                }
            }.build()
    }

    private fun generateRequestSpecFun(
        ep: ApiEndpointDO,
        ctx: ClientGenerationContext,
    ): FunSpec {
        val builder =
            FunSpec
                .builder("${ep.generatedName}Request")
                .addModifiers(KModifier.PRIVATE)
                .returns(RESPONSE_SPEC_T)

        ep.params.forEach { param ->
            builder.addParameter(
                ParameterSpec.builder(param.generatedName, param.type.toTypeName(ctx.typeNames)).build(),
            )
        }
        ep.requestBody?.let { body ->
            builder.addParameter(
                ParameterSpec.builder(body.generatedName, body.type.toTypeName(ctx.typeNames)).build(),
            )
        }

        builder.addCode(buildRequestSpecCode(ep, ctx))
        return builder.build()
    }

    private fun generateBodyMethod(
        ep: ApiEndpointDO,
        ctx: TypeNameContext,
    ): FunSpec {
        val returnType = RestClientApiPolicy.bodyReturnType(ep, ctx)
        val builder =
            overrideFunBuilder(ep, ctx)
                .returns(returnType)

        if (RestClientApiPolicy.hasBody(ep)) {
            builder.addCode(
                "return requireNotNull(%L.body(%L)) { %S }\n",
                requestCall(ep),
                typeReferenceCode(ep, ctx),
                "Expected response body for ${ep.generatedName}",
            )
        } else {
            builder.addCode("%L.toBodilessEntity()\n", requestCall(ep))
        }

        return builder.build()
    }

    private fun generateHttpInfoMethod(
        ep: ApiEndpointDO,
        ctx: TypeNameContext,
    ): FunSpec {
        val builder =
            overrideFunBuilder(ep, ctx, suffix = "WithHttpInfo")
                .returns(RestClientApiPolicy.httpInfoReturnType(ep, ctx))

        if (RestClientApiPolicy.hasBody(ep)) {
            builder.addCode(
                "return %L.toEntity(%L)\n",
                requestCall(ep),
                typeReferenceCode(ep, ctx),
            )
        } else {
            builder.addCode("return %L.toBodilessEntity()\n", requestCall(ep))
        }

        return builder.build()
    }

    private fun generateResponseSpecMethod(
        ep: ApiEndpointDO,
        ctx: TypeNameContext,
    ): FunSpec =
        overrideFunBuilder(ep, ctx, suffix = "WithResponseSpec")
            .returns(RestClientApiPolicy.responseSpecReturnType())
            .addCode("return %L\n", requestCall(ep))
            .build()

    private fun overrideFunBuilder(
        ep: ApiEndpointDO,
        ctx: TypeNameContext?,
        suffix: String = "",
    ): FunSpec.Builder {
        val builder =
            FunSpec
                .builder(ep.generatedName + suffix)
                .addModifiers(KModifier.OVERRIDE)

        ep.params.forEach { param ->
            val parameterType =
                param.type
                    .toTypeName(
                        requireNotNull(ctx) {
                            "TypeNameContext required for ${ep.generatedName}$suffix"
                        },
                    ).copy(
                        nullable = param.rawParam.location == RawPathDO.ParamLocationDO.HEADER || param.type.nullable,
                    )
            builder.addParameter(
                ParameterSpec
                    .builder(param.generatedName, parameterType)
                    .build(),
            )
        }

        ep.requestBody?.let { body ->
            builder.addParameter(
                ParameterSpec
                    .builder(
                        body.generatedName,
                        body.type.toTypeName(
                            requireNotNull(ctx) {
                                "TypeNameContext required for ${ep.generatedName}$suffix"
                            },
                        ),
                    ).build(),
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

    private fun buildRequestSpecCode(
        ep: ApiEndpointDO,
        ctx: ClientGenerationContext,
    ): CodeBlock {
        val builder = CodeBlock.builder()
        val methodRef = httpMethodRef(ep)

        if (!ctx.encodePathParameterSlashes) {
            builder.addStatement("var resolvedPath = resolvePath(%S)", ep.rawOperation.path)
            ep.params
                .filter { it.rawParam.location == RawPathDO.ParamLocationDO.PATH }
                .forEach { param ->
                    val value =
                        if ((param.type as? TrivialTypeDO)?.kind == TrivialTypeDO.Kind.STRING) {
                            CodeBlock.of("%L", param.generatedName)
                        } else {
                            CodeBlock.of("%L.toString()", param.generatedName)
                        }
                    builder.addStatement(
                        "resolvedPath = resolvedPath.replace(%S, %L)",
                        "{${param.rawParam.name}}",
                        value,
                    )
                }
        }

        builder.addStatement("val request = restClient.method(%T.%L)", HTTP_METHOD_T, methodRef)
        builder.add("request.uri { builder ->\n")
        builder.indent()
        if (ctx.encodePathParameterSlashes) {
            builder.addStatement("builder.path(resolvePath(%S))", ep.rawOperation.path)
        } else {
            builder.addStatement("builder.path(resolvedPath)")
        }
        addQueryParams(builder, ep)
        addPathBuild(builder, ep, ctx)
        builder.unindent()
        builder.add("}\n")

        addHeaderParams(builder, ep)

        ep.requestBody?.let { body ->
            val bodyType = body.type.toTypeName(ctx.typeNames)
            if (bodyType.isNullable) {
                builder.addStatement("%L?.let(request::body)", body.generatedName)
            } else {
                builder.addStatement("request.body(%L)", body.generatedName)
            }
        }

        builder.addStatement("return request.retrieve()")
        return builder.build()
    }

    private fun typeReferenceCode(
        ep: ApiEndpointDO,
        ctx: TypeNameContext,
    ): CodeBlock =
        CodeBlock.of(
            "object : %T<%T>() {}",
            PARAMETERIZED_TYPE_REFERENCE_T,
            ep.successResponse!!.type!!.toTypeName(ctx),
        )
}

private fun requestCall(ep: ApiEndpointDO): String =
    buildString {
        append("${ep.generatedName}Request(")
        append(
            buildList {
                ep.params.forEach { add(it.generatedName) }
                ep.requestBody?.let { add(it.generatedName) }
            }.joinToString(", "),
        )
        append(")")
    }

private fun httpMethodRef(ep: ApiEndpointDO): String =
    when (ep.rawOperation.httpMethod) {
        RawPathDO.HttpMethodDO.GET -> "GET"
        RawPathDO.HttpMethodDO.POST -> "POST"
        RawPathDO.HttpMethodDO.PUT -> "PUT"
        RawPathDO.HttpMethodDO.PATCH -> "PATCH"
        RawPathDO.HttpMethodDO.DELETE -> "DELETE"
    }

private fun addQueryParams(
    builder: CodeBlock.Builder,
    ep: ApiEndpointDO,
) {
    ep.params
        .filter { it.rawParam.location == RawPathDO.ParamLocationDO.QUERY }
        .forEach { param ->
            when (param.type) {
                is ListTypeDO -> addListQueryParam(builder, param)
                else -> addScalarQueryParam(builder, param)
            }
        }
}

private fun addListQueryParam(
    builder: CodeBlock.Builder,
    param: ApiParamDO,
) {
    val listType = param.type as ListTypeDO
    val itemCode =
        if (listType.elementType is TrivialTypeDO &&
            (listType.elementType as TrivialTypeDO).kind == TrivialTypeDO.Kind.STRING
        ) {
            "it"
        } else {
            "it.toString()"
        }

    if (param.rawParam.required) {
        builder.addStatement(
            "%L.forEach { builder.queryParam(%S, $itemCode) }",
            param.generatedName,
            param.rawParam.name,
        )
    } else {
        builder.addStatement(
            "%L?.forEach { builder.queryParam(%S, $itemCode) }",
            param.generatedName,
            param.rawParam.name,
        )
    }
}

private fun addScalarQueryParam(
    builder: CodeBlock.Builder,
    param: ApiParamDO,
) {
    val valueCode = optionalValueCode(param)
    if (param.rawParam.required) {
        builder.addStatement(
            "builder.queryParam(%S, %L.toString())",
            param.rawParam.name,
            param.generatedName,
        )
    } else {
        builder.addStatement(
            "%L?.let { builder.queryParam(%S, $valueCode) }",
            param.generatedName,
            param.rawParam.name,
        )
    }
}

private fun addPathBuild(
    builder: CodeBlock.Builder,
    ep: ApiEndpointDO,
    ctx: ClientGenerationContext,
) {
    val pathParams = ep.params.filter { it.rawParam.location == RawPathDO.ParamLocationDO.PATH }
    if (pathParams.isEmpty() || !ctx.encodePathParameterSlashes) {
        builder.addStatement("builder.build()")
        return
    }

    builder.add("builder.build(mapOf(\n")
    builder.indent()
    pathParams.forEachIndexed { index, param ->
        val suffix = if (index == pathParams.lastIndex) "" else ","
        val value =
            if ((param.type as? TrivialTypeDO)?.kind == TrivialTypeDO.Kind.STRING) {
                CodeBlock.of("%L", param.generatedName)
            } else {
                CodeBlock.of("%L.toString()", param.generatedName)
            }
        builder.add("%S to %L$suffix\n", param.rawParam.name, value)
    }
    builder.unindent()
    builder.add("))\n")
}

private fun addHeaderParams(
    builder: CodeBlock.Builder,
    ep: ApiEndpointDO,
) {
    val headerParams = ep.params.filter { it.rawParam.location == RawPathDO.ParamLocationDO.HEADER }
    if (headerParams.isEmpty()) return

    builder.add("request.headers { headers ->\n")
    builder.indent()
    headerParams.forEach { param ->
        val valueCode = optionalValueCode(param)
        builder.addStatement(
            "%L?.let { headers.add(%S, $valueCode) }",
            param.generatedName,
            param.rawParam.name,
        )
    }
    builder.unindent()
    builder.add("}\n")
}

private fun optionalValueCode(param: ApiParamDO): String {
    val trivialType = param.type as? TrivialTypeDO
    return if (trivialType?.kind == TrivialTypeDO.Kind.STRING) {
        "it"
    } else {
        "it.toString()"
    }
}
