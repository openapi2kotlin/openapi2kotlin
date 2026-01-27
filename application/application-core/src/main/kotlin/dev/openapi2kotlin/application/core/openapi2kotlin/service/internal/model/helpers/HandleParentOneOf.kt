package dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.model.helpers

import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.ModelDO

internal fun List<ModelDO>.handleParentOneOf() {
    val byName = associateBy { it.rawSchema.originalName }

    // oneOf: parent has oneOfChildren, child has parentOneOf.
    forEach { parent ->
        parent.rawSchema.oneOfChildren.forEach { childName ->
            val child = byName[childName] ?: return@forEach
            child.parentOneOf += parent.rawSchema.originalName
        }
    }
}
