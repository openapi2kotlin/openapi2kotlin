package dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.model.helpers

import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.ModelDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.ModelShapeDO
import dev.openapi2kotlin.application.usecase.openapi2kotlin.OpenApi2KotlinUseCase

internal fun List<ModelDO>.handleDefaultValues(cfg: OpenApi2KotlinUseCase.ModelConfig) {
    applyNullableDefaults()
    if (!cfg.defaultDiscriminatorValue) return

    val bySchemaName: Map<String, ModelDO> = associateBy { it.rawSchema.originalName }
    forEach { model -> model.applyDiscriminatorDefault(bySchemaName) }
}

internal fun ModelDO.findNearestDiscriminatorParent(bySchemaName: Map<String, ModelDO>): ModelDO? {
    val visited = mutableSetOf<String>()
    val queue = ArrayDeque(rawSchema.allOfParents)

    while (queue.isNotEmpty()) {
        val parentName = queue.removeFirst()
        if (visited.add(parentName)) {
            val parent = bySchemaName[parentName]
            if (parent?.rawSchema?.discriminatorPropertyName != null) return parent
            parent?.rawSchema?.allOfParents?.let(queue::addAll)
        }
    }

    return null
}

internal fun ModelDO.selfDiscriminatorValueFromOwnMapping(): String? =
    rawSchema.discriminatorMapping
        .entries
        .firstOrNull { (_, ref) -> ref.substringAfterLast('/') == rawSchema.originalName }
        ?.key

private fun List<ModelDO>.applyNullableDefaults() {
    forEach { model ->
        model.fields =
            model.fields
                .map { field ->
                    when {
                        field.defaultValueCode != null -> field
                        !field.required && field.type.nullable -> field.copy(defaultValueCode = "null")
                        else -> field
                    }
                }.toMutableList()
    }
}

private fun ModelDO.applyDiscriminatorDefault(bySchemaName: Map<String, ModelDO>) {
    if (!isConcreteInstantiable()) return

    val parentWithDisc = findNearestDiscriminatorParent(bySchemaName)
    val discriminatorName = discriminatorName(parentWithDisc) ?: return
    val discriminatorValue = discriminatorValue(parentWithDisc)

    fields =
        fields
            .map { field ->
                if (field.originalName == discriminatorName && field.defaultValueCode == null) {
                    field.copy(defaultValueCode = "\"$discriminatorValue\"")
                } else {
                    field
                }
            }.toMutableList()
}

private fun ModelDO.discriminatorName(parentWithDisc: ModelDO?): String? =
    rawSchema.discriminatorPropertyName ?: parentWithDisc?.rawSchema?.discriminatorPropertyName

private fun ModelDO.discriminatorValue(parentWithDisc: ModelDO?): String =
    selfDiscriminatorValueFromOwnMapping()
        ?: parentWithDisc
            ?.polymorphism
            ?.schemaNameToDiscriminatorValue
            ?.get(rawSchema.originalName)
        ?: rawSchema.originalName

private fun ModelDO.isConcreteInstantiable(): Boolean =
    modelShape is ModelShapeDO.DataClass ||
        modelShape is ModelShapeDO.OpenClass ||
        modelShape is ModelShapeDO.EmptyClass
