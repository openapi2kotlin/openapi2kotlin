package dev.openapi2kotlin.adapter.generateapi.internal

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.UNIT
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.FieldTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.ListTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.RefTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.TrivialTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.server.ServerApiDO
import java.nio.file.Path

fun generate(
    apis: List<ServerApiDO>,
    serverPackageName: String,
    modelPackageName: String,
    outputDirPath: Path,
) {
    val outDirFile = outputDirPath.toFile()

    apis.forEach { api ->
        val rawPath = api.rawPath
        val typeBuilder = TypeSpec.interfaceBuilder(rawPath.interfaceName)

        rawPath.operations.forEach { op ->
            val funBuilder = FunSpec.builder(op.operationId)
                .addModifiers(KModifier.ABSTRACT, KModifier.SUSPEND)

            op.parameters.forEach { p ->
                val typeName = p.type.toTypeName(modelPackageName)
                funBuilder.addParameter(ParameterSpec.builder(p.name, typeName).build())
            }

            op.requestBody?.let { body ->
                val typeName = body.type.toTypeName(modelPackageName)
                funBuilder.addParameter(ParameterSpec.builder("body", typeName).build())
            }

            val returnType: TypeName =
                op.successResponse?.type?.toTypeName(modelPackageName) ?: UNIT

            funBuilder.returns(returnType)

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

        FileSpec.builder(serverPackageName, rawPath.interfaceName)
            .addType(typeBuilder.build())
            .build()
            .writeTo(outDirFile)
    }

    outDirFile.walkTopDown()
        .filter { it.isFile && it.extension == "kt" }
        .forEach { file ->
            file.writeText(file.readText().replace("public ", ""))
        }
}

/* ---------- FieldTypeDO -> KotlinPoet.TypeName ---------- */

private val STRING = ClassName("kotlin", "String")
private val INT = ClassName("kotlin", "Int")
private val LONG = ClassName("kotlin", "Long")
private val FLOAT = ClassName("kotlin", "Float")
private val DOUBLE = ClassName("kotlin", "Double")
private val BOOLEAN = ClassName("kotlin", "Boolean")
private val ANY = ClassName("kotlin", "Any")
private val BYTE_ARRAY = ClassName("kotlin", "ByteArray")

private val BIG_DECIMAL = ClassName("java.math", "BigDecimal")
private val LOCAL_DATE = ClassName("java.time", "LocalDate")
private val OFFSET_DATE_TIME = ClassName("java.time", "OffsetDateTime")

private val LIST = ClassName("kotlin.collections", "List")

private fun TrivialTypeDO.Kind.typeName(): ClassName = when (this) {
    TrivialTypeDO.Kind.STRING -> STRING
    TrivialTypeDO.Kind.INT -> INT
    TrivialTypeDO.Kind.LONG -> LONG
    TrivialTypeDO.Kind.FLOAT -> FLOAT
    TrivialTypeDO.Kind.DOUBLE -> DOUBLE
    TrivialTypeDO.Kind.BIG_DECIMAL -> BIG_DECIMAL
    TrivialTypeDO.Kind.BOOLEAN -> BOOLEAN
    TrivialTypeDO.Kind.LOCAL_DATE -> LOCAL_DATE
    TrivialTypeDO.Kind.OFFSET_DATE_TIME -> OFFSET_DATE_TIME
    TrivialTypeDO.Kind.BYTE_ARRAY -> BYTE_ARRAY
    TrivialTypeDO.Kind.ANY -> ANY
}

private fun FieldTypeDO.toTypeName(modelPackageName: String): TypeName = when (this) {
    is TrivialTypeDO ->
        kind.typeName().copy(nullable = nullable)
    is RefTypeDO -> {
        val cls = ClassName(modelPackageName, cleanSchemaName(schemaName))
        cls.copy(nullable = nullable)
    }
    is ListTypeDO -> {
        val elementTypeName = elementType.toTypeName(modelPackageName)
        LIST.parameterizedBy(elementTypeName).copy(nullable = nullable)
    }
}

private fun cleanSchemaName(name: String): String =
    name.replace("_", "")
