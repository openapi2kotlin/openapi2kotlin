package dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.model.helpers

import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.ModelDO

internal fun List<ModelDO>.handleAllOfChildren() {
    val byName = associateBy { it.rawSchema.originalName }

    forEach { child ->
        child.rawSchema.allOfParents.forEach { parentName ->
            val parent = byName[parentName]
            parent?.allOfChildren?.add(child.generatedName)
        }
    }
}
