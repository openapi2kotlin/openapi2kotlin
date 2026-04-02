package dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.model.helpers

import dev.openapi2kotlin.application.core.openapi2kotlin.domain.model.FieldDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.model.ListTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.model.ModelDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.model.RefTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.raw.RawSchemaDO

internal fun ModelDO.findInheritedPropertySchema(
    field: FieldDO,
    bySchemaName: Map<String, ModelDO>,
): RawSchemaDO.SchemaPropertyDO? {
    val visited = mutableSetOf<String>()
    val queue = ArrayDeque(rawSchema.allOfParents)

    while (queue.isNotEmpty()) {
        val parentName = queue.removeFirst()
        if (visited.add(parentName)) {
            val parent = bySchemaName[parentName]
            val inheritedProperty = parent?.rawSchema?.ownProperties?.get(field.originalName)
            if (inheritedProperty != null) return inheritedProperty
            parent?.rawSchema?.allOfParents?.let(queue::addAll)
        }
    }

    return null
}

internal fun shouldAddValidForList(
    listType: ListTypeDO,
    bySchemaName: Map<String, ModelDO>,
): Boolean {
    val elementType = listType.elementType as? RefTypeDO
    return elementType != null &&
        bySchemaName[elementType.schemaName]?.rawSchema?.enumValues?.isEmpty() != false
}

internal fun FieldDO.shouldAddValid(bySchemaName: Map<String, ModelDO>): Boolean =
    when (val currentType = type) {
        is RefTypeDO -> bySchemaName[currentType.schemaName]?.rawSchema?.enumValues?.isEmpty() != false
        is ListTypeDO -> shouldAddValidForList(currentType, bySchemaName)
        else -> false
    }

internal fun String.toKotlinStringLiteral(): String =
    buildString {
        append('"')
        for (ch in this@toKotlinStringLiteral) {
            when (ch) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(ch)
            }
        }
        append('"')
    }

internal data class ValidationAnnotationNames(
    val namespace: String,
) {
    val valid: String = "$namespace.validation.Valid"
    val notNull: String = "$namespace.validation.constraints.NotNull"
    val size: String = "$namespace.validation.constraints.Size"
    val pattern: String = "$namespace.validation.constraints.Pattern"
    val decimalMin: String = "$namespace.validation.constraints.DecimalMin"
    val decimalMax: String = "$namespace.validation.constraints.DecimalMax"
}
