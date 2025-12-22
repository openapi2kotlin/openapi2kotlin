package dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.model.helpers

import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.FieldTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.ListTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.ModelDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.ModelShapeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.RefTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.TrivialTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.raw.RawSchemaDO

internal fun List<ModelDO>.handleModelShape() {
    val byName = associateBy { it.rawSchema.originalName }

    forEach { component ->
        if (component.rawSchema.isArraySchema && component.rawSchema.arrayItemType != null) {
            component.modelShape = ModelShapeDO.TypeAlias(
                target = ListTypeDO(
                    elementType = component.rawSchema.arrayItemType.toFieldTypeDO(),
                    nullable = false,
                )
            )
            return@forEach
        }

        if (component.rawSchema.enumValues.isNotEmpty()) {
            component.modelShape = ModelShapeDO.EnumClass(
                values = component.rawSchema.enumValues,
            )
            return@forEach
        }

        val hasOneOf = component.rawSchema.oneOfChildren.isNotEmpty()
        val isOneOfPolymorphic = hasOneOf
        val isAbstractAllOfBase =
            component.allOfChildren.isNotEmpty() &&
                    !component.rawSchema.usedInPaths &&
                    !component.rawSchema.usedAsProperty

        component.modelShape = when {
            isOneOfPolymorphic || isAbstractAllOfBase ->
                ModelShapeDO.SealedInterface(extends = emptyList())

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

    forEach { component ->
        when (component.modelShape) {
            is ModelShapeDO.EnumClass,
            is ModelShapeDO.TypeAlias -> return@forEach
            else -> {}
        }

        val allOfParents = component.rawSchema.allOfParents
        var parentClass: String? = null
        val parentInterfaces = mutableListOf<String>()

        allOfParents.forEach { parentName ->
            val parent = byName[parentName] ?: return@forEach
            when (parent.modelShape) {
                is ModelShapeDO.SealedInterface -> if (!parentInterfaces.contains(parentName)) parentInterfaces += parentName
                is ModelShapeDO.OpenClass,
                is ModelShapeDO.DataClass -> if (parentClass == null) parentClass = parentName
                is ModelShapeDO.EnumClass,
                is ModelShapeDO.TypeAlias,
                is ModelShapeDO.Undecided -> {}
            }
        }

        component.parentOneOf?.let { parentName ->
            if (!parentInterfaces.contains(parentName)) parentInterfaces += parentName
        }

        component.modelShape = when (val shape = component.modelShape) {
            is ModelShapeDO.SealedInterface ->
                shape.copy(extends = parentInterfaces)

            is ModelShapeDO.DataClass ->
                shape.copy(extend = parentClass, implements = parentInterfaces)

            is ModelShapeDO.OpenClass ->
                shape.copy(extend = parentClass, implements = parentInterfaces)

            is ModelShapeDO.EnumClass,
            is ModelShapeDO.TypeAlias,
            is ModelShapeDO.Undecided ->
                shape
        }
    }
}

private fun RawSchemaDO.RawFieldTypeDO.toFieldTypeDO(): FieldTypeDO = when (this) {
    is RawSchemaDO.RawRefTypeDO ->
        RefTypeDO(schemaName = schemaName, nullable = nullable)

    is RawSchemaDO.RawArrayTypeDO ->
        ListTypeDO(elementType = elementType.toFieldTypeDO(), nullable = nullable)

    is RawSchemaDO.RawPrimitiveTypeDO -> {
        val trivial = when (type) {
            RawSchemaDO.RawPrimitiveTypeDO.Type.STRING -> TrivialTypeDO.Kind.STRING
            RawSchemaDO.RawPrimitiveTypeDO.Type.BOOLEAN -> TrivialTypeDO.Kind.BOOLEAN
            RawSchemaDO.RawPrimitiveTypeDO.Type.INTEGER -> {
                if (format == "int64") TrivialTypeDO.Kind.LONG else TrivialTypeDO.Kind.INT
            }
            RawSchemaDO.RawPrimitiveTypeDO.Type.NUMBER -> TrivialTypeDO.Kind.DOUBLE
            RawSchemaDO.RawPrimitiveTypeDO.Type.OBJECT -> TrivialTypeDO.Kind.ANY
        }
        TrivialTypeDO(kind = trivial, nullable = nullable)
    }
}
