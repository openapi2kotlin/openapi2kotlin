package dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.model.helpers

import dev.openapi2kotlin.application.core.openapi2kotlin.domain.model.ModelDO

internal fun List<ModelDO>.handleParentOneOf() {
    val byName = associateBy { it.rawSchema.originalName }

    // oneOf: parent has oneOfChildren, child has parentOneOf.
    forEach { parent ->
        parent.rawSchema.oneOfChildren.forEach { childName ->
            val child = byName[childName] ?: return@forEach
            child.parentOneOf += parent.rawSchema.originalName
        }
    }

    moveWiderOneOfParentsToNarrowerInterfaces(byName)
    keepMostSpecificOneOfParents(byName)
}

private fun List<ModelDO>.moveWiderOneOfParentsToNarrowerInterfaces(byName: Map<String, ModelDO>) {
    forEach { narrower ->
        val narrowerChildren = narrower.rawSchema.oneOfChildren.toSet()
        if (narrowerChildren.isEmpty()) return@forEach

        forEach { wider ->
            val widerChildren = wider.rawSchema.oneOfChildren.toSet()
            if (
                narrower.rawSchema.originalName != wider.rawSchema.originalName &&
                widerChildren.size > narrowerChildren.size &&
                widerChildren.containsAll(narrowerChildren)
            ) {
                byName[narrower.rawSchema.originalName]?.parentOneOf += wider.rawSchema.originalName
            }
        }
    }
}

private fun List<ModelDO>.keepMostSpecificOneOfParents(byName: Map<String, ModelDO>) {
    forEach { child ->
        val removableParents =
            child.parentOneOf.filter { candidateName ->
                val candidateChildren = byName[candidateName]?.rawSchema?.oneOfChildren?.toSet().orEmpty()
                child.parentOneOf.any { otherName ->
                    val otherChildren = byName[otherName]?.rawSchema?.oneOfChildren?.toSet().orEmpty()
                    candidateName != otherName &&
                        candidateChildren.size > otherChildren.size &&
                        candidateChildren.containsAll(otherChildren)
                }
            }

        child.parentOneOf.removeAll(removableParents.toSet())
    }
}
