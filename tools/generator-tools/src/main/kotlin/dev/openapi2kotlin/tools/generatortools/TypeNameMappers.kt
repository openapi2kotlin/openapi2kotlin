package dev.openapi2kotlin.tools.generatortools

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.*

private val STRING = ClassName("kotlin", "String")
private val INT = ClassName("kotlin", "Int")
private val LONG = ClassName("kotlin", "Long")
private val FLOAT = ClassName("kotlin", "Float")
private val DOUBLE = ClassName("kotlin", "Double")
private val BOOLEAN = ClassName("kotlin", "Boolean")
private val ANY = ClassName("kotlin", "Any")
private val BYTE_ARRAY = ClassName("kotlin", "ByteArray")
private val LIST = ClassName("kotlin.collections", "List")

private val BIG_DECIMAL = ClassName("java.math", "BigDecimal")
private val LOCAL_DATE = ClassName("java.time", "LocalDate")
private val OFFSET_DATE_TIME = ClassName("java.time", "OffsetDateTime")

data class TypeNameContext(
    /**
     * Package where referenced model types live (e.g. config.model.packageName).
     */
    val modelPackageName: String,

    /**
     * Map of schema originalName -> ModelDO (optional but improves name resolution).
     */
    val bySchemaName: Map<String, ModelDO> = emptyMap(),
)

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

fun FieldTypeDO.toTypeName(ctx: TypeNameContext): TypeName = when (this) {
    is TrivialTypeDO ->
        kind.typeName().copy(nullable = nullable)

    is RefTypeDO -> {
        val target = ctx.bySchemaName[schemaName]
        ClassName(ctx.modelPackageName, target?.generatedName ?: schemaName)
            .copy(nullable = nullable)
    }

    is ListTypeDO ->
        LIST.parameterizedBy(elementType.toTypeName(ctx))
            .copy(nullable = nullable)
}