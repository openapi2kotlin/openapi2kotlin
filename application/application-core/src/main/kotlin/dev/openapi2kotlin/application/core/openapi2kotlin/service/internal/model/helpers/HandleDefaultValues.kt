package dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.model.helpers

import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.ModelDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.ModelShapeDO
import dev.openapi2kotlin.application.usecase.openapi2kotlin.OpenApi2KotlinUseCase

internal fun List<ModelDO>.handleDefaultValues(
    cfg: OpenApi2KotlinUseCase.ModelConfig,
) {
    /* ---------------------------------------------------------------------
     * 1) Default null values for optional nullable fields
     * ------------------------------------------------------------------- */

    forEach { model ->
        model.fields = model.fields
            .map { field ->
                when {
                    field.defaultValueCode != null -> field
                    !field.required && field.type.nullable -> field.copy(defaultValueCode = "null")
                    else -> field
                }
            }
            .toMutableList()
    }

    /* ---------------------------------------------------------------------
     * 2) Default discriminator value on concrete instantiable types
     * ------------------------------------------------------------------- */

    if (!cfg.defaultDiscriminatorValue) return

    val bySchemaName: Map<String, ModelDO> = associateBy { it.rawSchema.originalName }

    forEach { model ->
        val isConcrete =
            model.modelShape is ModelShapeDO.DataClass ||
                model.modelShape is ModelShapeDO.OpenClass ||
                model.modelShape is ModelShapeDO.EmptyClass

        if (!isConcrete) return@forEach

        val parentWithDisc = model.findNearestDiscriminatorParent(bySchemaName)
        val discOriginal =
            model.rawSchema.discriminatorPropertyName
                ?: parentWithDisc?.rawSchema?.discriminatorPropertyName
                ?: return@forEach

        val discValue =
            model.selfDiscriminatorValueFromOwnMapping()
                ?: parentWithDisc?.polymorphism
                    ?.schemaNameToDiscriminatorValue
                    ?.get(model.rawSchema.originalName)
                ?: model.rawSchema.originalName

        model.fields = model.fields.map { field ->
            if (field.originalName == discOriginal && field.defaultValueCode == null) {
                field.copy(defaultValueCode = "\"$discValue\"")
            } else {
                field
            }
        }.toMutableList()
    }
}

internal fun ModelDO.findNearestDiscriminatorParent(
    bySchemaName: Map<String, ModelDO>,
): ModelDO? {
    val visited = mutableSetOf<String>()
    val queue = ArrayDeque(rawSchema.allOfParents)

    while (queue.isNotEmpty()) {
        val parentName = queue.removeFirst()
        if (!visited.add(parentName)) continue

        val parent = bySchemaName[parentName] ?: continue
        if (parent.rawSchema.discriminatorPropertyName != null) return parent

        queue.addAll(parent.rawSchema.allOfParents)
    }

    return null
}

internal fun ModelDO.selfDiscriminatorValueFromOwnMapping(): String? =
    rawSchema.discriminatorMapping
        .entries
        .firstOrNull { (_, ref) -> ref.substringAfterLast('/') == rawSchema.originalName }
        ?.key
