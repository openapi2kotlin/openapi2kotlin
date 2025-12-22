package dev.openapi2kotlin.adapter.generatemodel.internal.helpers

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.FieldTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.ListTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.ModelDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.PrimitiveTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.RefTypeDO

private val STRING = ClassName("kotlin", "String")
private val INT = ClassName("kotlin", "Int")
private val LONG = ClassName("kotlin", "Long")
private val DOUBLE = ClassName("kotlin", "Double")
private val BOOLEAN = ClassName("kotlin", "Boolean")
private val ANY = ClassName("kotlin", "Any")
private val LIST = ClassName("kotlin.collections", "List")

internal fun ModelDO.className(): ClassName =
    ClassName(packageName, generatedName)

private fun PrimitiveTypeDO.PrimitiveTypeNameDO.typeName(): ClassName = when (this) {
    PrimitiveTypeDO.PrimitiveTypeNameDO.STRING -> STRING
    PrimitiveTypeDO.PrimitiveTypeNameDO.INT -> INT
    PrimitiveTypeDO.PrimitiveTypeNameDO.LONG -> LONG
    PrimitiveTypeDO.PrimitiveTypeNameDO.DOUBLE -> DOUBLE
    PrimitiveTypeDO.PrimitiveTypeNameDO.BOOLEAN -> BOOLEAN
    PrimitiveTypeDO.PrimitiveTypeNameDO.ANY -> ANY
}

internal fun FieldTypeDO.typeName(
    owner: ModelDO,
    bySchemaName: Map<String, ModelDO>,
): TypeName = when (this) {
    is PrimitiveTypeDO ->
        name.typeName().copy(nullable = nullable)

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