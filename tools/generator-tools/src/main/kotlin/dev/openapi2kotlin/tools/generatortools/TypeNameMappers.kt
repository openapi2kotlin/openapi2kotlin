package dev.openapi2kotlin.tools.generatortools

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.FieldTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.ListTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.ModelDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.RefTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.TrivialTypeDO

private val STRING = ClassName("kotlin", "String")
private val INT = ClassName("kotlin", "Int")
private val LONG = ClassName("kotlin", "Long")
private val FLOAT = ClassName("kotlin", "Float")
private val DOUBLE = ClassName("kotlin", "Double")
private val BOOLEAN = ClassName("kotlin", "Boolean")
private val ANY = ClassName("kotlin", "Any")
private val BYTE_ARRAY = ClassName("kotlin", "ByteArray")
private val LIST = ClassName("kotlin.collections", "List")
private val JSON_ELEMENT = ClassName("kotlinx.serialization.json", "JsonElement")

private val BIG_DECIMAL = ClassName("java.math", "BigDecimal")
private val JAVA_LOCAL_DATE = ClassName("java.time", "LocalDate")
private val JAVA_OFFSET_DATE_TIME = ClassName("java.time", "OffsetDateTime")
private val KOTLINX_LOCAL_DATE = ClassName("kotlinx.datetime", "LocalDate")
private val KOTLIN_TIME_INSTANT = ClassName("kotlin.time", "Instant")

data class TypeNameContext(
    /**
     * Package where referenced model types live (e.g. config.model.packageName).
     */
    val modelPackageName: String,

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
    TrivialTypeDO.Kind.JAVA_LOCAL_DATE -> JAVA_LOCAL_DATE
    TrivialTypeDO.Kind.KOTLINX_LOCAL_DATE -> KOTLINX_LOCAL_DATE
    TrivialTypeDO.Kind.OFFSET_DATE_TIME -> JAVA_OFFSET_DATE_TIME
    TrivialTypeDO.Kind.INSTANT -> KOTLIN_TIME_INSTANT
    TrivialTypeDO.Kind.BYTE_ARRAY -> BYTE_ARRAY
    TrivialTypeDO.Kind.JSON_ELEMENT -> JSON_ELEMENT
    TrivialTypeDO.Kind.ANY -> ANY
}

fun FieldTypeDO.toTypeName(ctx: TypeNameContext): TypeName = when (this) {
    is TrivialTypeDO ->
        kind.typeName().copy(nullable = nullable)

    is RefTypeDO -> {
        val target = ctx.bySchemaName[schemaName]
        val className = if (target != null) {
            ClassName(target.packageName, target.generatedName)
        } else {
            ClassName(ctx.modelPackageName, schemaName.toFallbackSimpleName())
        }
        className
            .copy(nullable = nullable)
    }

    is ListTypeDO ->
        LIST.parameterizedBy(elementType.toTypeName(ctx))
            .copy(nullable = nullable)
}

private fun String.toFallbackSimpleName(): String =
    substringAfterLast('.')
        .replace(Regex("[^A-Za-z0-9]"), "")
        .ifBlank { "Model" }
        .let { if (it.first().isDigit()) "_$it" else it }
