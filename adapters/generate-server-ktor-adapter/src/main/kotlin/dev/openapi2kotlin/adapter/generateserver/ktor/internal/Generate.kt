package dev.openapi2kotlin.adapter.generateserver.ktor.internal

import buildKtorRoute
import com.squareup.kotlinpoet.*
import dev.openapi2kotlin.adapter.tools.TypeNameContext
import dev.openapi2kotlin.adapter.tools.addImportsAndShortenArgs
import dev.openapi2kotlin.adapter.tools.postProcess
import dev.openapi2kotlin.adapter.tools.shortenArgs
import dev.openapi2kotlin.adapter.tools.toTypeName
import dev.openapi2kotlin.application.core.openapi2kotlin.model.api.ApiAnnotationDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.api.ApiDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.ModelDO
import java.nio.file.Path

fun generate(
    apis: List<ApiDO>,
    serverPackageName: String,
    modelPackageName: String,
    outputDirPath: Path,
    models: List<ModelDO>,
) {
    val outDir = outputDirPath.toFile()
    val bySchemaName: Map<String, ModelDO> = models.associateBy { it.rawSchema.originalName }
    val ctx = TypeNameContext(modelPackageName = modelPackageName, bySchemaName = bySchemaName)

    apis.forEach { api ->
        val typeBuilder = TypeSpec.interfaceBuilder(api.generatedName)

        api.annotations.shortenArgs().forEach { typeBuilder.addAnnotation(it.toPoet()) }

        api.endpoints.forEach { ep ->
            val funBuilder = FunSpec.builder(ep.generatedName)
                .addModifiers(KModifier.ABSTRACT, KModifier.SUSPEND)

            ep.annotations.shortenArgs().forEach { funBuilder.addAnnotation(it.toPoet()) }

            ep.params.forEach { p ->
                val pb = ParameterSpec.builder(
                    p.generatedName,
                    p.type.toTypeName(ctx),
                )
                p.annotations.shortenArgs().forEach { pb.addAnnotation(it.toPoet()) }
                funBuilder.addParameter(pb.build())
            }

            ep.requestBody?.let { body ->
                val pb = ParameterSpec.builder(
                    body.generatedName,
                    body.type.toTypeName(ctx),
                )
                body.annotations.shortenArgs().forEach { pb.addAnnotation(it.toPoet()) }
                funBuilder.addParameter(pb.build())
            }

            funBuilder.returns(ep.successResponse?.type?.toTypeName(ctx) ?: UNIT)

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

        FileSpec.builder(serverPackageName, api.generatedName)
            .addImportsAndShortenArgs(allAnnotationsOriginal)
            .addType(typeBuilder.build())
            .build()
            .writeTo(outDir)

        // Ktor routing is adapter-specific: infer basePath + generate a Route.() extension function.
        val basePath = inferBasePath(api)

        val apiStem = api.generatedName.removeSuffix("Api").ifBlank { api.generatedName }
        val routesFileName = apiStem + "Routes"
        val routesFunName = apiStem.replaceFirstChar { it.lowercaseChar() } + "Routes"

        FileSpec.builder(serverPackageName, routesFileName)
            .addFunction(
                buildKtorRoute(
                    api = api,
                    basePath = basePath,
                    routesFunName = routesFunName,
                    serverPackageName = serverPackageName,
                    ctx = ctx,
                )
            )
            .build()
            .writeTo(outDir)
    }

    outDir.postProcess()
}

private fun inferBasePath(api: ApiDO): String {
    // Best-effort: choose the shortest concrete path and take its first segment.
    val shortest = api.rawPath.operations
        .map { it.path.trim() }
        .filter { it.isNotBlank() }
        .minByOrNull { it.length }
        ?: "/"

    val seg = shortest.trim('/').split('/').firstOrNull().orEmpty()
    return "/" + seg
}

private fun ApiAnnotationDO.toPoet(): AnnotationSpec {
    val b = AnnotationSpec.builder(ClassName.bestGuess(fqName))
    argsCode.forEach { b.addMember("%L", it) }
    return b.build()
}
