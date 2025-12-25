package dev.openapi2kotlin.adapter.generateserver.internal

import buildKtorRoute
import com.squareup.kotlinpoet.*
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.ModelDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.server.ServerAnnotationDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.server.ServerApiDO
import java.nio.file.Path

fun generate(
    apis: List<ServerApiDO>,
    serverPackageName: String,
    modelPackageName: String,
    outputDirPath: Path,
    models: List<ModelDO>,
) {
    val outDir = outputDirPath.toFile()
    val bySchemaName: Map<String, ModelDO> = models.associateBy { it.rawSchema.originalName }

    apis.forEach { api ->
        val typeBuilder = TypeSpec.interfaceBuilder(api.generatedName)

        api.annotations.forEach { typeBuilder.addAnnotation(it.toPoet()) }

        api.endpoints.forEach { ep ->
            val funBuilder = FunSpec.builder(ep.generatedName)
                .addModifiers(KModifier.ABSTRACT, KModifier.SUSPEND)

            ep.annotations.forEach { funBuilder.addAnnotation(it.toPoet()) }

            ep.params.forEach { p ->
                val pb = ParameterSpec.builder(p.generatedName, p.type.toTypeName(modelPackageName, bySchemaName))
                p.annotations.forEach { pb.addAnnotation(it.toPoet()) }
                funBuilder.addParameter(pb.build())
            }

            ep.requestBody?.let { body ->
                val pb = ParameterSpec.builder(body.generatedName, body.type.toTypeName(modelPackageName, bySchemaName))
                body.annotations.forEach { pb.addAnnotation(it.toPoet()) }
                funBuilder.addParameter(pb.build())
            }

            funBuilder.returns(ep.successResponse?.type?.toTypeName(modelPackageName, bySchemaName) ?: UNIT)

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

        FileSpec.builder(serverPackageName, api.generatedName)
            .addType(typeBuilder.build())
            .build()
            .writeTo(outDir)

        api.route?.let { route ->
            FileSpec.builder(serverPackageName, route.generatedFileName)
                .addFunction(
                    buildKtorRoute(
                        api = api,
                        route = route,
                        serverPackageName = serverPackageName,
                        modelPackageName = modelPackageName,
                        bySchemaName = bySchemaName,
                    )
                )
                .build()
                .writeTo(outDir)
        }
    }

    outDir.walkTopDown()
        .filter { it.isFile && it.extension == "kt" }
        .forEach { file ->
            val text = file.readText()
            file.writeText(text
                // KotlinPoet add redundant public modifiers by default
                .replace("public ", "")
                // KotlinPoet escapes this package segment; we prefer the normal form.
                .replace(".`annotation`.", ".annotation.")
            )
        }
}

private fun ServerAnnotationDO.toPoet(): AnnotationSpec {
    val b = AnnotationSpec.builder(ClassName.bestGuess(fqName))
    argsCode.forEach { b.addMember("%L", it) }
    return b.build()
}