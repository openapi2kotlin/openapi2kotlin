package dev.openapi2kotlin.adapter.generateclient.ktor

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import dev.openapi2kotlin.application.core.openapi2kotlin.model.api.ApiDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.api.ApiEndpointDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.ListTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.ModelDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.TrivialTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.raw.RawPathDO
import dev.openapi2kotlin.application.core.openapi2kotlin.port.GenerateApiPort
import dev.openapi2kotlin.tools.apigenerator.ApiPolicy
import dev.openapi2kotlin.tools.generatortools.TypeNameContext
import dev.openapi2kotlin.tools.generatortools.postProcess
import dev.openapi2kotlin.tools.generatortools.toTypeName

private val HTTP_CLIENT_T = ClassName("io.ktor.client", "HttpClient")
private val M_body = com.squareup.kotlinpoet.MemberName("io.ktor.client.call", "body")
private val M_get = com.squareup.kotlinpoet.MemberName("io.ktor.client.request", "get")
private val M_post = com.squareup.kotlinpoet.MemberName("io.ktor.client.request", "post")
private val M_put = com.squareup.kotlinpoet.MemberName("io.ktor.client.request", "put")
private val M_patch = com.squareup.kotlinpoet.MemberName("io.ktor.client.request", "patch")
private val M_delete = com.squareup.kotlinpoet.MemberName("io.ktor.client.request", "delete")
private val M_appendPathSegments = com.squareup.kotlinpoet.MemberName("io.ktor.http", "appendPathSegments")
private val M_parameter = com.squareup.kotlinpoet.MemberName("io.ktor.client.request", "parameter")
private val M_setBody = com.squareup.kotlinpoet.MemberName("io.ktor.client.request", "setBody")

internal class ApiImplGenerator : GenerateApiPort {
    override fun generateApi(command: GenerateApiPort.Command) {
        val outDir = command.outputDirPath.toFile()
        val bySchemaName: Map<String, ModelDO> = command.models.associateBy { it.rawSchema.originalName }
        val ctx = TypeNameContext(modelPackageName = command.modelPackageName, bySchemaName = bySchemaName)

        command.apiContext.apis.forEach { api ->
            FileSpec.builder(command.packageName, "${api.generatedName}Impl")
                .addType(generateApiImpl(api, command.packageName, ctx))
                .build()
                .writeTo(outDir)
        }

        outDir.postProcess()
    }

    private fun generateApiImpl(
        api: ApiDO,
        apiPackageName: String,
        ctx: TypeNameContext,
    ): TypeSpec {
        val apiType = ClassName(apiPackageName, api.generatedName)

        return TypeSpec.classBuilder("${api.generatedName}Impl")
            .addSuperinterface(apiType)
            .primaryConstructor(
                FunSpec.constructorBuilder()
                    .addParameter("client", HTTP_CLIENT_T)
                    .build()
            )
            .addProperty(
                PropertySpec.builder("client", HTTP_CLIENT_T, KModifier.PRIVATE)
                    .initializer("client")
                    .build()
            )
            .apply {
                api.endpoints.forEach { addFunction(generateEndpointFun(it, ctx)) }
            }
            .build()
    }

    private fun generateEndpointFun(
        ep: ApiEndpointDO,
        ctx: TypeNameContext,
    ): FunSpec {
        val returnType = ApiPolicy.Default.returnType(ep, ctx)
        val requestCall = buildRequestCall(ep, ctx)
        val methodBuilder = FunSpec.builder(ep.generatedName)
            .addModifiers(KModifier.OVERRIDE, KModifier.SUSPEND)
            .returns(returnType)

        ep.params.forEach { param ->
            methodBuilder.addParameter(
                ParameterSpec.builder(param.generatedName, param.type.toTypeName(ctx)).build()
            )
        }

        ep.requestBody?.let { body ->
            methodBuilder.addParameter(
                ParameterSpec.builder(body.generatedName, body.type.toTypeName(ctx)).build()
            )
        }

        if (returnType == com.squareup.kotlinpoet.UNIT) {
            methodBuilder.addCode("%L\n", requestCall)
        } else {
            methodBuilder.addCode("return %L\n", requestCall)
        }

        return methodBuilder.build()
    }

    private fun buildRequestCall(
        ep: ApiEndpointDO,
        ctx: TypeNameContext,
    ): CodeBlock {
        val httpMethod = when (ep.rawOperation.httpMethod) {
            RawPathDO.HttpMethodDO.GET -> M_get
            RawPathDO.HttpMethodDO.POST -> M_post
            RawPathDO.HttpMethodDO.PUT -> M_put
            RawPathDO.HttpMethodDO.PATCH -> M_patch
            RawPathDO.HttpMethodDO.DELETE -> M_delete
        }

        return CodeBlock.builder()
            .add("client.%M {\n", httpMethod)
            .indent()
            .addPathBlock(ep)
            .addQueryBlock(ep)
            .addHeaderBlock(ep)
            .addBodyBlock(ep)
            .unindent()
            .add("}")
            .apply {
                ep.successResponse?.type?.let { successType ->
                    add(".%M<%T>()", M_body, successType.toTypeName(ctx))
                }
            }
            .build()
    }

    private fun CodeBlock.Builder.addPathBlock(ep: ApiEndpointDO): CodeBlock.Builder {
        val segments = ep.rawOperation.path
            .trim('/')
            .split('/')
            .filter { it.isNotBlank() }

        add("url {\n")
        indent()
        segments.forEach { segment ->
            val rawParamName = segment.removeSurrounding("{", "}")
            val pathParam = ep.params.firstOrNull {
                it.rawParam.location == RawPathDO.ParamLocationDO.PATH && it.rawParam.name == rawParamName
            }

            if (pathParam != null) {
                val trivialType = pathParam.type as? TrivialTypeDO
                if (trivialType?.kind == TrivialTypeDO.Kind.STRING) {
                    addStatement("%M(%L)", M_appendPathSegments, pathParam.generatedName)
                } else {
                    addStatement("%M(%L.toString())", M_appendPathSegments, pathParam.generatedName)
                }
            } else {
                addStatement("%M(%S)", M_appendPathSegments, segment)
            }
        }
        unindent()
        add("}\n")
        return this
    }

    private fun CodeBlock.Builder.addQueryBlock(ep: ApiEndpointDO): CodeBlock.Builder {
        ep.params
            .filter { it.rawParam.location == RawPathDO.ParamLocationDO.QUERY }
            .forEach { param ->
                when (param.type) {
                    is ListTypeDO -> {
                        val listType = param.type as ListTypeDO
                        val itemCode = if (listType.elementType is TrivialTypeDO &&
                            (listType.elementType as TrivialTypeDO).kind == TrivialTypeDO.Kind.STRING
                        ) {
                            "it"
                        } else {
                            "it.toString()"
                        }
                        addStatement("%L?.forEach { %M(%S, $itemCode) }", param.generatedName, M_parameter, param.rawParam.name)
                    }
                    else -> {
                        val trivialType = param.type as? TrivialTypeDO
                        val valueCode = if (trivialType?.kind == TrivialTypeDO.Kind.STRING) {
                            "it"
                        } else {
                            "it.toString()"
                        }
                        if (param.rawParam.required) {
                            if (trivialType?.kind == TrivialTypeDO.Kind.STRING) {
                                addStatement("%M(%S, %L)", M_parameter, param.rawParam.name, param.generatedName)
                            } else {
                                addStatement("%M(%S, %L.toString())", M_parameter, param.rawParam.name, param.generatedName)
                            }
                        } else {
                            addStatement("%L?.let { %M(%S, $valueCode) }", param.generatedName, M_parameter, param.rawParam.name)
                        }
                    }
                }
            }
        return this
    }

    private fun CodeBlock.Builder.addHeaderBlock(ep: ApiEndpointDO): CodeBlock.Builder {
        ep.params
            .filter { it.rawParam.location == RawPathDO.ParamLocationDO.HEADER }
            .forEach { param ->
                val trivialType = param.type as? TrivialTypeDO
                val valueCode = if (trivialType?.kind == TrivialTypeDO.Kind.STRING) {
                    "it"
                } else {
                    "it.toString()"
                }
                if (param.rawParam.required) {
                    if (trivialType?.kind == TrivialTypeDO.Kind.STRING) {
                        addStatement("headers.append(%S, %L)", param.rawParam.name, param.generatedName)
                    } else {
                        addStatement("headers.append(%S, %L.toString())", param.rawParam.name, param.generatedName)
                    }
                } else {
                    addStatement("%L?.let { headers.append(%S, $valueCode) }", param.generatedName, param.rawParam.name)
                }
            }
        return this
    }

    private fun CodeBlock.Builder.addBodyBlock(ep: ApiEndpointDO): CodeBlock.Builder {
        ep.requestBody?.let { body ->
            addStatement("%M(%L)", M_setBody, body.generatedName)
        }
        return this
    }
}
