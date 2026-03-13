package dev.openapi2kotlin.tools.apigenerator

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.WildcardTypeName
import dev.openapi2kotlin.application.core.openapi2kotlin.model.api.ApiAnnotationDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.api.ApiDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.ModelDO
import dev.openapi2kotlin.application.core.openapi2kotlin.port.GenerateApiPort
import dev.openapi2kotlin.tools.apigenerator.internal.formatFunParams
import dev.openapi2kotlin.tools.generatortools.TypeNameContext
import dev.openapi2kotlin.tools.generatortools.addImportsAndShortenArgs
import dev.openapi2kotlin.tools.generatortools.postProcess
import dev.openapi2kotlin.tools.generatortools.resolveImportAliases
import dev.openapi2kotlin.tools.generatortools.shortenArgs
import dev.openapi2kotlin.tools.generatortools.toTypeName

class ApiGenerator(
    private val policy: ApiPolicy = ApiPolicy.Default,
): GenerateApiPort {
    override fun generateApi(command: GenerateApiPort.Command) {
        val outDir = command.outputDirPath.toFile()
        val bySchemaName: Map<String, ModelDO> = command.models.associateBy { it.rawSchema.originalName }
        val ctx = TypeNameContext(
            modelPackageName = command.modelPackageName,
            bySchemaName = bySchemaName,
        )

        command.apiContext.apis.forEach { api ->
            val typeBuilder = TypeSpec.interfaceBuilder(api.generatedName)
            val allAnnotationsOriginal: List<ApiAnnotationDO> = buildList {
                addAll(api.annotations)
                api.endpoints.forEach { ep ->
                    addAll(ep.annotations)
                    ep.params.forEach { addAll(it.annotations) }
                    ep.requestBody?.let { addAll(it.annotations) }
                }
            }
            val aliases = resolveImportAliases(
                annotations = allAnnotationsOriginal,
                reservedSimpleNames = api.usedSimpleNames(ctx, policy),
            )

            api.annotations.shortenArgs(aliases).forEach { typeBuilder.addAnnotation(it.toPoet(aliases)) }

            api.endpoints.forEach { ep ->
                val funBuilder = FunSpec.builder(ep.generatedName)
                    .addModifiers(KModifier.ABSTRACT)
                    .apply {
                        if (policy.suspendFunctions) addModifiers(KModifier.SUSPEND)
                    }

                ep.annotations.shortenArgs(aliases).forEach { funBuilder.addAnnotation(it.toPoet(aliases)) }

                ep.params.forEach { p ->
                    val pb = ParameterSpec.builder(p.generatedName, p.type.toTypeName(ctx))
                    p.annotations.shortenArgs(aliases).forEach { pb.addAnnotation(it.toPoet(aliases)) }
                    funBuilder.addParameter(pb.build())
                }

                ep.requestBody?.let { body ->
                    val pb = ParameterSpec.builder(body.generatedName, body.type.toTypeName(ctx))
                    body.annotations.shortenArgs(aliases).forEach { pb.addAnnotation(it.toPoet(aliases)) }
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

            FileSpec.builder(command.packageName, api.generatedName)
                .addImportsAndShortenArgs(allAnnotationsOriginal, aliases)
                .addType(typeBuilder.build())
                .build()
                .writeTo(outDir)
        }

        outDir.postProcess()
        outDir.formatFunParams()
    }

    private fun ApiAnnotationDO.toPoet(aliases: Map<String, String>): AnnotationSpec {
        val annotationType = aliases[fqName]?.let { alias -> ClassName("", alias) } ?: ClassName.bestGuess(fqName)
        val b = AnnotationSpec.builder(annotationType)
        argsCode.forEach { b.addMember("%L", it) }
        return b.build()
    }

    private fun ApiDO.usedSimpleNames(
        ctx: TypeNameContext,
        policy: ApiPolicy,
    ): Set<String> {
        val usedSimpleNames = buildSet {
            endpoints.forEach { ep ->
                addAll(policy.returnType(ep, ctx).usedSimpleNames())
                ep.params.forEach { param ->
                    addAll(param.type.toTypeName(ctx).usedSimpleNames())
                }
                ep.requestBody?.let { body ->
                    addAll(body.type.toTypeName(ctx).usedSimpleNames())
                }
            }
        }
        return usedSimpleNames
    }

    private fun TypeName.usedSimpleNames(): List<String> = when (this) {
        is ClassName -> listOf(simpleName)
        is ParameterizedTypeName -> listOf(rawType.simpleName) + typeArguments.flatMap { it.usedSimpleNames() }
        is WildcardTypeName -> inTypes.flatMap { it.usedSimpleNames() } + outTypes.flatMap { it.usedSimpleNames() }
        is TypeVariableName -> bounds.flatMap { it.usedSimpleNames() }
        is LambdaTypeName -> parameters.flatMap { it.type.usedSimpleNames() } + returnType.usedSimpleNames()
        else -> emptyList()
    }
}