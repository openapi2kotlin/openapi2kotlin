package dev.openapi2kotlin.adapter.generateclient.http4k

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.TypeSpec
import dev.openapi2kotlin.application.core.openapi2kotlin.model.api.ApiDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.api.ApiEndpointDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.ModelDO
import dev.openapi2kotlin.application.core.openapi2kotlin.port.GenerateApiPort
import dev.openapi2kotlin.tools.generatortools.TypeNameContext
import dev.openapi2kotlin.tools.generatortools.postProcess
import dev.openapi2kotlin.tools.generatortools.toTypeName

internal class Http4kClientInterfaceGenerator : GenerateApiPort {
    override fun generateApi(command: GenerateApiPort.Command) {
        val outDir = command.outputDirPath.toFile()
        val bySchemaName: Map<String, ModelDO> = command.models.associateBy { it.rawSchema.originalName }
        val ctx =
            TypeNameContext(
                modelPackageName = command.modelPackageName,
                bySchemaName = bySchemaName,
            )

        command.apiContext.apis.forEach { api ->
            FileSpec.builder(command.packageName, api.generatedName)
                .indent("    ")
                .addType(generateApiInterface(api, ctx))
                .build()
                .writeTo(outDir)
        }

        outDir.postProcess()
    }

    private fun generateApiInterface(
        api: ApiDO,
        ctx: TypeNameContext,
    ): TypeSpec =
        TypeSpec.interfaceBuilder(api.generatedName)
            .apply {
                api.endpoints.forEach { endpoint ->
                    addFunction(generateBodyMethod(endpoint, ctx))
                    addFunction(generateHttpInfoMethod(endpoint, ctx))
                }
            }
            .build()

    private fun generateBodyMethod(
        ep: ApiEndpointDO,
        ctx: TypeNameContext,
    ): FunSpec =
        baseFunBuilder(ep, ctx)
            .returns(Http4kClientApiPolicy.bodyReturnType(ep, ctx))
            .build()

    private fun generateHttpInfoMethod(
        ep: ApiEndpointDO,
        ctx: TypeNameContext,
    ): FunSpec =
        baseFunBuilder(ep, ctx, suffix = "WithHttpInfo")
            .returns(Http4kClientApiPolicy.httpInfoReturnType())
            .build()

    private fun baseFunBuilder(
        ep: ApiEndpointDO,
        ctx: TypeNameContext,
        suffix: String = "",
    ): FunSpec.Builder {
        val builder =
            FunSpec.builder(ep.generatedName + suffix)
                .addModifiers(KModifier.ABSTRACT)

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
}
