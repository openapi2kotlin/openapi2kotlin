package dev.openapi2kotlin.tools.apigenerator

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.TypeSpec
import dev.openapi2kotlin.application.core.openapi2kotlin.model.api.ApiAnnotationDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.ModelDO
import dev.openapi2kotlin.application.core.openapi2kotlin.port.GenerateApiPort
import dev.openapi2kotlin.tools.apigenerator.internal.formatFunParams
import dev.openapi2kotlin.tools.generatortools.TypeNameContext
import dev.openapi2kotlin.tools.generatortools.addImportsAndShortenArgs
import dev.openapi2kotlin.tools.generatortools.postProcess
import dev.openapi2kotlin.tools.generatortools.shortenArgs
import dev.openapi2kotlin.tools.generatortools.toTypeName

class ApiGenerator(
    private val policy: ApiPolicy = ApiPolicy.Default,
): GenerateApiPort {
    override fun generateApi(command: GenerateApiPort.Command) {
        val outDir = command.outputDirPath.toFile()
        val bySchemaName: Map<String, ModelDO> = command.models.associateBy { it.rawSchema.originalName }
        val ctx = TypeNameContext(modelPackageName = command.modelPackageName, bySchemaName = bySchemaName)

        command.apiContext.apis.forEach { api ->
            val typeBuilder = TypeSpec.interfaceBuilder(api.generatedName)

            api.annotations.shortenArgs().forEach { typeBuilder.addAnnotation(it.toPoet()) }

            api.endpoints.forEach { ep ->
                val funBuilder = FunSpec.builder(ep.generatedName)
                    .addModifiers(KModifier.ABSTRACT)
                    .apply {
                        if (policy.suspendFunctions) addModifiers(KModifier.SUSPEND)
                    }

                ep.annotations.shortenArgs().forEach { funBuilder.addAnnotation(it.toPoet()) }

                ep.params.forEach { p ->
                    val pb = ParameterSpec.builder(p.generatedName, p.type.toTypeName(ctx))
                    p.annotations.shortenArgs().forEach { pb.addAnnotation(it.toPoet()) }
                    funBuilder.addParameter(pb.build())
                }

                ep.requestBody?.let { body ->
                    val pb = ParameterSpec.builder(body.generatedName, body.type.toTypeName(ctx))
                    body.annotations.shortenArgs().forEach { pb.addAnnotation(it.toPoet()) }
                    funBuilder.addParameter(pb.build())
                }

                funBuilder.returns(policy.returnType(ep, ctx))

                val kdoc = buildString {
                    ep.rawOperation.summary?.let { appendLine(it) }
                    ep.rawOperation.description?.let {
                        if (isNotEmpty()) appendLine()
                        appendLine(it)
                    }
                }
                if (kdoc.isNotBlank()) funBuilder.addKdoc(kdoc)

                typeBuilder.addFunction(funBuilder.build())
            }

            val allAnnotationsOriginal: List<ApiAnnotationDO> = buildList {
                addAll(api.annotations)
                api.endpoints.forEach { ep ->
                    addAll(ep.annotations)
                    ep.params.forEach { addAll(it.annotations) }
                    ep.requestBody?.let { addAll(it.annotations) }
                }
            }

            FileSpec.builder(command.packageName, api.generatedName)
                .addImportsAndShortenArgs(allAnnotationsOriginal)
                .addType(typeBuilder.build())
                .build()
                .writeTo(outDir)
        }

        outDir.postProcess()
        outDir.formatFunParams()
    }

    private fun ApiAnnotationDO.toPoet(): AnnotationSpec {
        val b = AnnotationSpec.builder(ClassName.bestGuess(fqName))
        argsCode.forEach { b.addMember("%L", it) }
        return b.build()
    }
}