package dev.openapi2kotlin.adapter.generateapi.internal

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.ListTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.PrimitiveTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.RefTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.FieldTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.server.ServerApiDO
import java.nio.file.Path

/**
 * Generate pure Kotlin interfaces like CategoryApi, ProductOfferingApi, ...
 *
 * No Ktor/Spring dependencies here.
 */
fun generate(
    apis: List<ServerApiDO>,
    serverPackageName: String,
    modelPackageName: String,
    outputDirPath: Path,
) {
    val outDirFile = outputDirPath.toFile()

    apis.forEach { api ->
        val rawPath = api.rawPath
        val typeBuilder = TypeSpec.Companion.interfaceBuilder(rawPath.interfaceName)

        rawPath.operations.forEach { op ->
            val funBuilder = FunSpec.Companion.builder(op.operationId)
                .addModifiers(KModifier.ABSTRACT, KModifier.SUSPEND) // <-- ABSTRACT here

            // parameters (path + query + header, request body at the end)
            op.parameters.forEach { p ->
                val typeName = p.type.toTypeName(modelPackageName)
                funBuilder.addParameter(
                    ParameterSpec.builder(p.name, typeName).build()
                )
            }

            op.requestBody?.let { body ->
                val typeName = body.type.toTypeName(modelPackageName)
                funBuilder.addParameter(
                    ParameterSpec.builder("body", typeName).build()
                )
            }

            // return type = main success response or Unit
            val returnType: TypeName =
                op.successResponse?.type?.toTypeName(modelPackageName)
                    ?: UNIT

            funBuilder.returns(returnType)

            // optional KDoc from summary/description
            val kdoc = buildString {
                op.summary?.let { appendLine(it) }
                op.description?.let {
                    if (isNotEmpty()) appendLine()
                    appendLine(it)
                }
            }
            if (kdoc.isNotBlank()) {
                funBuilder.addKdoc(kdoc)
            }

            typeBuilder.addFunction(funBuilder.build())
        }

        val fileSpec = FileSpec.builder(serverPackageName, rawPath.interfaceName)
            .addType(typeBuilder.build())
            .build()

        fileSpec.writeTo(outDirFile)
    }

    // ---- remove explicit "public " ----
    outDirFile.walkTopDown()
        .filter { it.isFile && it.extension == "kt" }
        .forEach { file ->
            val modified = file.readText().replace("public ", "")
            file.writeText(modified)
        }
}

/* ---------- DtoType -> KotlinPoet.TypeName ---------- */

private val STRING = ClassName("kotlin", "String")
private val INT = ClassName("kotlin", "Int")
private val LONG = ClassName("kotlin", "Long")
private val DOUBLE = ClassName("kotlin", "Double")
private val BOOLEAN = ClassName("kotlin", "Boolean")
private val ANY = ClassName("kotlin", "Any")
private val LIST = ClassName("kotlin.collections", "List")

private fun PrimitiveTypeDO.PrimitiveTypeNameDO.typeName(): ClassName = when (this) {
    PrimitiveTypeDO.PrimitiveTypeNameDO.STRING -> STRING
    PrimitiveTypeDO.PrimitiveTypeNameDO.INT -> INT
    PrimitiveTypeDO.PrimitiveTypeNameDO.LONG -> LONG
    PrimitiveTypeDO.PrimitiveTypeNameDO.DOUBLE -> DOUBLE
    PrimitiveTypeDO.PrimitiveTypeNameDO.BOOLEAN -> BOOLEAN
    PrimitiveTypeDO.PrimitiveTypeNameDO.ANY -> ANY
}

private fun FieldTypeDO.toTypeName(dtoPackageName: String): TypeName = when (this) {
    is PrimitiveTypeDO -> name.typeName().copy(nullable = nullable)
    is RefTypeDO -> {
        val cls = ClassName(dtoPackageName, cleanSchemaName(schemaName))
        cls.copy(nullable = nullable)
    }
    is ListTypeDO -> {
        val elementTypeName = elementType.toTypeName(dtoPackageName)
        LIST.parameterizedBy(elementTypeName).copy(nullable = nullable)
    }
}

/**
 * Same cleaning as in DTO generator to map schema name -> Kotlin class name.
 */
private fun cleanSchemaName(name: String): String =
    name.replace("_", "")
