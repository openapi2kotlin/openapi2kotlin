package dev.openapi2kotlin.tools.apigenerator

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.WildcardTypeName
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.api.ApiAnnotationDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.api.ApiDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.api.ApiEndpointDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.api.ApiParamDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.api.ApiRequestBodyDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.raw.RawPathDO
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
) : GenerateApiPort {
    override fun generateApi(command: GenerateApiPort.Command) {
        val outDir = command.outputDirPath.toFile()
        val ctx = command.toTypeNameContext()

        command.apiContext.apis.forEach { api ->
            val typeBuilder = TypeSpec.interfaceBuilder(api.generatedName)
            val allAnnotationsOriginal = api.collectAnnotations()
            val aliases =
                resolveImportAliases(
                    annotations = allAnnotationsOriginal,
                    reservedSimpleNames = api.usedSimpleNames(ctx, policy),
                )

            api.annotations.shortenArgs(aliases).forEach { typeBuilder.addAnnotation(it.toPoet(aliases)) }
            api.endpoints
                .map { endpoint -> endpoint.toFunSpec(ctx = ctx, aliases = aliases, policy = policy) }
                .forEach(typeBuilder::addFunction)

            FileSpec.builder(command.packageName, api.generatedName)
                .indent("    ")
                .addImportsAndShortenArgs(allAnnotationsOriginal, aliases)
                .addType(typeBuilder.build())
                .build()
                .writeTo(outDir)
        }

        outDir.postProcess()
        outDir.formatFunParams()
    }

    private fun GenerateApiPort.Command.toTypeNameContext(): TypeNameContext =
        TypeNameContext(
            modelPackageName = modelPackageName,
            bySchemaName = models.associateBy { it.rawSchema.originalName },
        )

    private fun ApiDO.collectAnnotations(): List<ApiAnnotationDO> =
        buildList {
            addAll(annotations)
            endpoints.forEach { endpoint ->
                addAll(endpoint.annotations)
                endpoint.params.forEach { addAll(it.annotations) }
                endpoint.requestBody?.let { addAll(it.annotations) }
            }
        }

    private fun ApiEndpointDO.toFunSpec(
        ctx: TypeNameContext,
        aliases: Map<String, String>,
        policy: ApiPolicy,
    ): FunSpec {
        val funBuilder =
            FunSpec.builder(generatedName)
                .addModifiers(KModifier.ABSTRACT)
                .apply {
                    if (policy.suspendFunctions) addModifiers(KModifier.SUSPEND)
                }

        annotations.shortenArgs(aliases).forEach { funBuilder.addAnnotation(it.toPoet(aliases)) }
        params.forEach { param ->
            funBuilder.addParameter(param.toParameterSpec(ctx, aliases))
        }
        requestBody?.let { body ->
            funBuilder.addParameter(body.toParameterSpec(ctx, aliases))
        }
        funBuilder.returns(policy.returnType(this, ctx))
        rawOperation.toKdoc().takeIf(String::isNotBlank)?.let(funBuilder::addKdoc)
        return funBuilder.build()
    }

    private fun ApiParamDO.toParameterSpec(
        ctx: TypeNameContext,
        aliases: Map<String, String>,
    ): ParameterSpec {
        val builder = ParameterSpec.builder(generatedName, type.toTypeName(ctx))
        annotations.shortenArgs(aliases).forEach { builder.addAnnotation(it.toPoet(aliases)) }
        return builder.build()
    }

    private fun ApiRequestBodyDO.toParameterSpec(
        ctx: TypeNameContext,
        aliases: Map<String, String>,
    ): ParameterSpec {
        val builder = ParameterSpec.builder(generatedName, type.toTypeName(ctx))
        annotations.shortenArgs(aliases).forEach { builder.addAnnotation(it.toPoet(aliases)) }
        return builder.build()
    }

    private fun RawPathDO.OperationDO.toKdoc(): String =
        buildString {
            summary?.let { appendLine(it) }
            description?.let {
                if (isNotEmpty()) appendLine()
                appendLine(it)
            }
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
        val usedSimpleNames =
            buildSet {
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

    private fun TypeName.usedSimpleNames(): List<String> =
        when (this) {
            is ClassName -> listOf(simpleName)
            is ParameterizedTypeName -> listOf(rawType.simpleName) + typeArguments.flatMap { it.usedSimpleNames() }
            is WildcardTypeName -> inTypes.flatMap { it.usedSimpleNames() } + outTypes.flatMap { it.usedSimpleNames() }
            is TypeVariableName -> bounds.flatMap { it.usedSimpleNames() }
            is LambdaTypeName -> parameters.flatMap { it.type.usedSimpleNames() } + returnType.usedSimpleNames()
            else -> emptyList()
        }
}
