package dev.openapi2kotlin.adapter.generatemodel.internal.helpers

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

private val BIG_DECIMAL = ClassName("java.math", "BigDecimal")
private val LOCAL_DATE = ClassName("java.time", "LocalDate")
private val OFFSET_DATE_TIME = ClassName("java.time", "OffsetDateTime")

internal fun ModelDO.className(): ClassName =
    ClassName(packageName, generatedName)

internal fun FieldTypeDO.typeName(
    owner: ModelDO,
    bySchemaName: Map<String, ModelDO>,
): TypeName = when (this) {
    is TrivialTypeDO -> {
        val base = when (kind) {
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
        base.copy(nullable = nullable)
    }

    is RefTypeDO -> {
        val target = bySchemaName[schemaName]
        val cls = target?.className() ?: ClassName(owner.packageName, schemaName)
        cls.copy(nullable = nullable)
    }

    is ListTypeDO -> {
        val elementTypeName = elementType.typeName(owner, bySchemaName)
        LIST.parameterizedBy(elementTypeName).copy(nullable = nullable)
    }
}