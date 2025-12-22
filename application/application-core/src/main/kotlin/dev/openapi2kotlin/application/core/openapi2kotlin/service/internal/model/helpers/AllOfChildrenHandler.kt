package dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.model.helpers

import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.ModelDO


internal fun allOfChildrenHandler(
    schemas: List<ModelDO>,
) {
    val byName = schemas.associateBy { it.rawSchema.originalName }

   schemas.forEach { child ->
        child.rawSchema.allOfParents.forEach { parentName ->
            val parent = byName[parentName]
            parent?.allOfChildren?.add(child.generatedName)
        }
    }
}
