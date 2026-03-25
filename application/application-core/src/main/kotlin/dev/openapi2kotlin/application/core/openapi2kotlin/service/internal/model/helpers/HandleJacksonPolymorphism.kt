package dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.model.helpers

import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.ModelAnnotationDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.ModelDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.ModelShapeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.PolymorphismDO
import dev.openapi2kotlin.application.usecase.openapi2kotlin.OpenApi2KotlinUseCase

internal fun ModelDO.buildPolymorphismMetadata(): PolymorphismMetadata? {
    val discriminatorName = rawSchema.discriminatorPropertyName
    val mappingValueToSchemaName = rawSchema.discriminatorMapping.mapValues { (_, ref) -> ref.substringAfterLast('/') }
    val childrenSchemaNames =
        if (discriminatorName != null) {
            resolvePolymorphicChildren(mappingValueToSchemaName)
        } else {
            emptyList()
        }

    return if (discriminatorName == null || childrenSchemaNames.isEmpty()) {
        null
    } else {
        PolymorphismMetadata(
            polymorphism =
                PolymorphismDO(
                    discriminatorPropertyOriginalName = discriminatorName,
                    discriminatorPropertyGeneratedName = discriminatorName.toKotlinName(),
                    schemaNameToDiscriminatorValue =
                        buildSchemaNameToDiscriminatorValue(
                            children = childrenSchemaNames,
                            mappingValueToSchemaName = mappingValueToSchemaName,
                        ),
                ),
            childrenSchemaNames = childrenSchemaNames,
            isOneOfWrapperSealedInterface =
                rawSchema.oneOfChildren.isNotEmpty() && modelShape is ModelShapeDO.SealedInterface,
        )
    }
}

internal fun ModelDO.buildPolymorphismAnnotations(
    metadata: PolymorphismMetadata,
    bySchemaName: Map<String, ModelDO>,
    cfg: OpenApi2KotlinUseCase.ModelConfig,
): List<ModelAnnotationDO> {
    val subtypeEntries = buildSubtypeEntries(metadata, bySchemaName)
    return buildList {
        if (cfg.jacksonStrictDiscriminatorSerialization) add(buildIgnorePropertiesAnnotation(metadata))
        add(buildJsonTypeInfoAnnotation(metadata.polymorphism.discriminatorPropertyOriginalName))
        add(ModelAnnotationDO(fqName = JSON_SUB_TYPES, argsCode = subtypeEntries))
    }
}

internal fun buildWrapperSubtypeEntries(
    wrapperSchemaName: String,
    wrapperPolymorphism: PolymorphismDO,
    bySchemaName: Map<String, ModelDO>,
): List<String> =
    buildList {
        wrapperPolymorphism.schemaNameToDiscriminatorValue
            .filterKeys { it != wrapperSchemaName }
            .forEach { (childSchemaName, discValue) ->
                val child = bySchemaName[childSchemaName] ?: return@forEach
                add("JsonSubTypes.Type(value = ${child.generatedName}::class, name = \"$discValue\")")
            }
    }

internal fun ModelDO.shouldAddReadOnlyDiscriminator(): Boolean =
    hasConcreteClassShape() &&
        rawSchema.oneOfChildren.isEmpty() &&
        allOfChildren.isEmpty()

private fun ModelDO.resolvePolymorphicChildren(mappingValueToSchemaName: Map<String, String>): List<String> {
    val isOneOfRoot = rawSchema.oneOfChildren.isNotEmpty()
    val hasMappingToOthers = mappingValueToSchemaName.values.any { it != rawSchema.originalName }

    return when {
        isOneOfRoot -> rawSchema.oneOfChildren
        hasMappingToOthers -> mappingValueToSchemaName.values.distinct().filter { it != rawSchema.originalName }
        else -> emptyList()
    }
}

private fun ModelDO.buildSubtypeEntries(
    metadata: PolymorphismMetadata,
    bySchemaName: Map<String, ModelDO>,
): List<String> =
    buildList {
        if (shouldIncludeSelfSubtype(metadata.childrenSchemaNames)) {
            val selfDiscriminatorValue =
                metadata.polymorphism.schemaNameToDiscriminatorValue[rawSchema.originalName] ?: rawSchema.originalName
            add(
                "JsonSubTypes.Type(value = $generatedName::class, " +
                    "name = \"$selfDiscriminatorValue\")",
            )
        }

        metadata.childrenSchemaNames.forEach { childSchemaName ->
            val child = bySchemaName[childSchemaName] ?: return@forEach
            val discriminatorValue =
                metadata.polymorphism.schemaNameToDiscriminatorValue[childSchemaName] ?: childSchemaName
            add(
                "JsonSubTypes.Type(value = ${child.generatedName}::class, " +
                    "name = \"$discriminatorValue\")",
            )
        }
    }

private fun ModelDO.shouldIncludeSelfSubtype(childrenSchemaNames: List<String>): Boolean =
    rawSchema.isDiscriminatorSelfMapped &&
        hasConcreteClassShape() &&
        childrenSchemaNames.isNotEmpty()

private fun buildIgnorePropertiesAnnotation(metadata: PolymorphismMetadata): ModelAnnotationDO =
    ModelAnnotationDO(
        fqName = JSON_IGNORE_PROPERTIES,
        argsCode =
            listOf(
                "value = [\"${metadata.polymorphism.discriminatorPropertyOriginalName}\"]",
                "allowSetters = true",
            ),
    )

private fun buildJsonTypeInfoAnnotation(discriminatorName: String): ModelAnnotationDO =
    ModelAnnotationDO(
        fqName = JSON_TYPE_INFO,
        argsCode =
            listOf(
                "use = JsonTypeInfo.Id.NAME",
                "include = JsonTypeInfo.As.PROPERTY",
                "property = \"$discriminatorName\"",
                "visible = true",
            ),
    )

internal fun ModelDO.buildSchemaNameToDiscriminatorValue(
    children: List<String>,
    mappingValueToSchemaName: Map<String, String>,
): Map<String, String> {
    val inverted =
        mappingValueToSchemaName.entries.associate { it.value to it.key }

    return buildMap {
        put(rawSchema.originalName, inverted[rawSchema.originalName] ?: rawSchema.originalName)
        children.forEach { schemaName ->
            put(schemaName, inverted[schemaName] ?: schemaName)
        }
    }
}
