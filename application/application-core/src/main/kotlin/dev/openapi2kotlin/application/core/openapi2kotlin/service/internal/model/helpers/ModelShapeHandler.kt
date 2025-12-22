package dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.model.helpers

import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.ModelShapeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.ListTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.ModelDO


/**
 * Third pass – decide GeneratedShape for each SchemaComponent.
 *
 * Rules:
 *
 * TypeAlias:
 *   - isArraySchema == true
 *
 * EnumClass:
 *   - enumValues is not empty
 *
 * SealedInterface:
 *   - has oneOf (oneOfChildren not empty), OR
 *   - (connectionDetails.allOfChildren not empty
 *      AND usedInPaths == false
 *      AND usedAsProperty == false)
 *
 * DataClass:
 *   - no allOfChildren  (and not matched as SealedInterface / EnumClass / TypeAlias)
 *
 * OpenClass:
 *   - has allOfChildren AND (usedInPaths OR usedAsProperty)
 *
 * For inheritance:
 *   - allOf parents:
 *       * SealedInterface parents → implements
 *       * DataClass/OpenClass parents → extend (first such wins)
 *   - parentOneOf (set when someone includes us in their oneOf) → implements that parent
 */
internal fun modelShapeHandler(schemas: List<ModelDO>) {
    val byName = schemas.associateBy { it.rawSchema.originalName }

    // Step 1: choose kind based on rules.
   schemas.forEach { component ->
        // 1) array schemas -> typealias, e.g. JsonPatchOperations = List<JsonPatch>
        if (component.rawSchema.isArraySchema && component.rawSchema.arrayItemType != null) {
            component.modelShape = ModelShapeDO.TypeAlias(
                target = ListTypeDO(
                    elementType = component.rawSchema.arrayItemType,
                    nullable = false,
                )
            )
            return@forEach
        }

        // 2) enums – leaf-ish by definition
        if (component.rawSchema.enumValues.isNotEmpty()) {
            component.modelShape = ModelShapeDO.EnumClass(
                values = component.rawSchema.enumValues,
            )
            return@forEach
        }


        val hasOneOf = component.rawSchema.oneOfChildren.isNotEmpty()
        val isOneOfPolymorphic = hasOneOf
        val isAbstractAllOfBase =
            component.allOfChildren.isNotEmpty() && !component.rawSchema.usedInPaths && !component.rawSchema.usedAsProperty

        component.modelShape = when {
            isOneOfPolymorphic || isAbstractAllOfBase ->
                ModelShapeDO.SealedInterface(
                    extends = emptyList(),
                )

            component.allOfChildren.isEmpty() ->
                ModelShapeDO.DataClass(
                    extend = null,
                    implements = emptyList(),
                )

            else ->
                ModelShapeDO.OpenClass(
                    extend = null,
                    implements = emptyList(),
                )
        }
    }

    // Step 2: fill extend / implements based on parents' shapes and parentOneOf.
   schemas.forEach { component ->
        // enums & typealiases don't participate in inheritance
        when (component.modelShape) {
            is ModelShapeDO.EnumClass,
            is ModelShapeDO.TypeAlias -> return@forEach
            else -> {}
        }

        val allOfParents = component.rawSchema.allOfParents
        var parentClass: String? = null
        val parentInterfaces = mutableListOf<String>()

        // inheritance from allOf
        allOfParents.forEach { parentName ->
            val parent = byName[parentName] ?: return@forEach
            when (parent.modelShape) {
                is ModelShapeDO.SealedInterface -> {
                    if (!parentInterfaces.contains(parentName)) {
                        parentInterfaces += parentName
                    }
                }

                is ModelShapeDO.OpenClass,
                is ModelShapeDO.DataClass ->
                    if (parentClass == null) parentClass = parentName

                is ModelShapeDO.EnumClass,
                is ModelShapeDO.TypeAlias,
                is ModelShapeDO.Undecided -> {
                    // ignore
                }
            }
        }

        // polymorphic parent from oneOf
        component.parentOneOf?.let { parentName ->
            if (!parentInterfaces.contains(parentName)) {
                parentInterfaces += parentName
            }
        }

        component.modelShape = when (val shape = component.modelShape) {
            is ModelShapeDO.SealedInterface ->
                shape.copy(extends = parentInterfaces)

            is ModelShapeDO.DataClass ->
                shape.copy(
                    extend = parentClass,
                    implements = parentInterfaces,
                )

            is ModelShapeDO.OpenClass ->
                shape.copy(
                    extend = parentClass,
                    implements = parentInterfaces,
                )

            is ModelShapeDO.EnumClass,
            is ModelShapeDO.TypeAlias,
            is ModelShapeDO.Undecided ->
                shape
        }
    }
}
