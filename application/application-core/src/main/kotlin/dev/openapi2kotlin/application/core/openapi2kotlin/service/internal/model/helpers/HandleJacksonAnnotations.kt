package dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.model.helpers

import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.ModelAnnotationDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.FieldDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.ModelDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.ModelShapeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.PolymorphismDO
import dev.openapi2kotlin.application.usecase.openapi2kotlin.OpenApi2KotlinUseCase

private const val JSON_PROPERTY = "com.fasterxml.jackson.annotation.JsonProperty"
private const val JSON_TYPE_INFO = "com.fasterxml.jackson.annotation.JsonTypeInfo"
private const val JSON_SUB_TYPES = "com.fasterxml.jackson.annotation.JsonSubTypes"
private const val JSON_IGNORE_PROPERTIES = "com.fasterxml.jackson.annotation.JsonIgnoreProperties"

internal fun List<ModelDO>.handleJacksonAnnotations(
    cfg: OpenApi2KotlinUseCase.ModelConfig.JacksonConfig,
) {
    if (!cfg.enabled) return

    val bySchemaName: Map<String, ModelDO> = associateBy { it.rawSchema.originalName }

    // 1) @JsonProperty for renamed fields
    if (cfg.jsonPropertyMapping) {
        forEach { model ->
            val useSite = model.jsonPropertyUseSite()

            model.fields = model.fields
                .map { field ->
                    if (field.originalName == field.generatedName) field
                    else field.addAnnotation(
                        ModelAnnotationDO(
                            useSite = useSite,
                            fqName = JSON_PROPERTY,
                            argsCode = listOf("\"${field.originalName}\""),
                        )
                    )
                }
                .toMutableList()
        }
    }

    // 2) Default null for optional nullable fields
    forEach { model ->
        model.fields = model.fields
            .map { f ->
                if (f.defaultValueCode != null) f
                else if (!f.required && f.type.nullable) f.copy(defaultValueCode = "null")
                else f
            }
            .toMutableList()
    }

    // 3) Polymorphism annotations:
    // - oneOf roots (union types)
    // - allOf roots (inheritance base with discriminator + mapping)
    forEach { parent ->
        val discOriginal = parent.rawSchema.discriminatorPropertyName ?: return@forEach
        val discGenerated = discOriginal.toKotlinName()

        // Determine children:
        //  - oneOf root: from oneOfChildren
        //  - allOf root: models that list this schema in allOfParents
        val childrenSchemaNames: List<String> = when {
            parent.rawSchema.oneOfChildren.isNotEmpty() ->
                parent.rawSchema.oneOfChildren

            else -> {
                // allOf inheritance children
                bySchemaName.values
                    .asSequence()
                    .filter { child -> child.rawSchema.allOfParents.contains(parent.rawSchema.originalName) }
                    .map { it.rawSchema.originalName }
                    .toList()
            }
        }

        // Not a polymorphic root -> skip
        if (childrenSchemaNames.isEmpty()) return@forEach

        // OpenAPI mapping semantics:
        // raw discriminatorValueToSchemaName: discriminator VALUE -> schema NAME
        // we want schema NAME -> discriminator VALUE
        val schemaNameToDiscriminatorValue = parent.buildSchemaNameToDiscriminatorValue(
            children = childrenSchemaNames
        )

        parent.polymorphism = PolymorphismDO(
            discriminatorPropertyOriginalName = discOriginal,
            discriminatorPropertyGeneratedName = discGenerated,
            schemaNameToDiscriminatorValue = schemaNameToDiscriminatorValue,
        )

        val typeAnnotations = buildList {
            if (cfg.strictDiscriminatorSerialization) {
                add(
                    ModelAnnotationDO(
                        fqName = JSON_IGNORE_PROPERTIES,
                        argsCode = listOf(
                            "value = [\"$discOriginal\"]",
                            "allowSetters = true",
                        ),
                    )
                )
            }

            add(
                ModelAnnotationDO(
                    fqName = JSON_TYPE_INFO,
                    argsCode = listOf(
                        "use = JsonTypeInfo.Id.NAME",
                        "include = JsonTypeInfo.As.PROPERTY",
                        "property = \"$discOriginal\"",
                        "visible = true",
                    ),
                )
            )

            val subtypeEntries: List<String> = childrenSchemaNames.mapNotNull { childSchemaName ->
                val child = bySchemaName[childSchemaName] ?: return@mapNotNull null
                val discValue = schemaNameToDiscriminatorValue[childSchemaName] ?: childSchemaName

                "JsonSubTypes.Type(value = ${child.generatedName}::class, name = \"$discValue\")"
            }

            add(
                ModelAnnotationDO(
                    fqName = JSON_SUB_TYPES,
                    argsCode = subtypeEntries,
                )
            )
        }

        parent.annotations = parent.annotations + typeAnnotations
    }

    // 4) Discriminator default on concrete LEAF classes (works for both:
    //    - leaf declares discriminator itself
    //    - leaf inherits discriminator from an allOf parent)
    if (cfg.defaultDiscriminatorValue) {
        forEach { model ->
            val isConcreteClass =
                model.modelShape is ModelShapeDO.DataClass || model.modelShape is ModelShapeDO.OpenClass
            if (!isConcreteClass) return@forEach

            // do not default union roots (oneOf parent)
            if (model.rawSchema.oneOfChildren.isNotEmpty()) return@forEach

            // avoid "base-ish" types (has allOf children)
            if (model.allOfChildren.isNotEmpty()) return@forEach

            // find discriminator field name + mapping source
            val parentWithDisc: ModelDO? = model.findNearestDiscriminatorParent(bySchemaName)
            val discOriginal: String? =
                model.rawSchema.discriminatorPropertyName
                    ?: parentWithDisc?.rawSchema?.discriminatorPropertyName

            if (discOriginal == null) return@forEach

            val discValue: String =
                parentWithDisc?.polymorphism
                    ?.schemaNameToDiscriminatorValue
                    ?.get(model.rawSchema.originalName)
                    ?: model.rawSchema.originalName

            model.fields = model.fields.map { f ->
                if (f.originalName == discOriginal && f.defaultValueCode == null) {
                    f.copy(defaultValueCode = "\"$discValue\"")
                } else f
            }.toMutableList()
        }
    }

    // 5) Make discriminator READ_ONLY on concrete, non-polymorphic leaves
    //    (ignore incoming @type; prevents "CategoryFVO" etc. being accepted)
    forEach { model ->
        val isConcreteClass =
            model.modelShape is ModelShapeDO.DataClass || model.modelShape is ModelShapeDO.OpenClass
        if (!isConcreteClass) return@forEach

        // union roots are polymorphic by definition
        if (model.rawSchema.oneOfChildren.isNotEmpty()) return@forEach

        // ignore for bases
        if (model.allOfChildren.isNotEmpty()) return@forEach

        val parentWithDisc: ModelDO? = model.findNearestDiscriminatorParent(bySchemaName)
        val discOriginal: String? =
            model.rawSchema.discriminatorPropertyName
                ?: parentWithDisc?.rawSchema?.discriminatorPropertyName
        if (discOriginal == null) return@forEach

        model.fields = model.fields.map { f ->
            if (f.originalName != discOriginal) f
            else f.addAnnotation(
                ModelAnnotationDO(
                    // must hit property/getter to influence deserialization
                    useSite = ModelAnnotationDO.UseSiteDO.GET,
                    fqName = JSON_PROPERTY,
                    argsCode = listOf("access = JsonProperty.Access.READ_ONLY"),
                )
            )
        }.toMutableList()
    }
}

/**
 * Decide whether JsonProperty should be @param: or @get: depending on model shape.
 */
private fun ModelDO.jsonPropertyUseSite(): ModelAnnotationDO.UseSiteDO = when (modelShape) {
    is ModelShapeDO.SealedInterface -> ModelAnnotationDO.UseSiteDO.GET
    else -> ModelAnnotationDO.UseSiteDO.PARAM
}

/**
 * Determines subtype schema names for polymorphism annotations.
 *
 * - If oneOfChildren present: use them (union root).
 * - Else if discriminator mapping present: use mapping targets (allOf base style).
 * - Else fallback: use allOfChildren (if your pipeline fills it with raw schema names).
 */
private fun ModelDO.polymorphicSubtypeSchemaNames(): List<String> {
    if (rawSchema.oneOfChildren.isNotEmpty()) return rawSchema.oneOfChildren

    if (rawSchema.discriminatorValueToSchemaName.isNotEmpty()) {
        // discriminatorValue -> schemaName
        return rawSchema.discriminatorValueToSchemaName.values
            .distinct()
            .filter { it != rawSchema.originalName }
    }

    // if your handleAllOfChildren stores raw schema names, this works;
    // if it stores generated names, fix handleAllOfChildren to store rawSchema.originalName instead.
    return allOfChildren
}

/**
 * Build schemaName -> discriminatorValue for a specific set of subtypes.
 *
 * - If mapping exists: invert it and fill missing with fallback schemaName.
 * - Else: discriminatorValue == schemaName
 */
private fun ModelDO.buildSchemaNameToDiscriminatorValue(children: List<String>): Map<String, String> {
    val mappingValueToSchema: Map<String, String> = rawSchema.discriminatorValueToSchemaName

    val inverted: Map<String, String> =
        if (mappingValueToSchema.isEmpty()) emptyMap()
        else mappingValueToSchema.entries.associate { (discValue, schemaName) ->
            schemaName to discValue
        }

    return buildMap(children.size) {
        for (schemaName in children) {
            put(schemaName, inverted[schemaName] ?: schemaName)
        }
    }
}

/**
 * Walk up allOf parents until we find one that declares discriminatorPropertyName.
 */
private fun ModelDO.findNearestDiscriminatorParent(bySchemaName: Map<String, ModelDO>): ModelDO? {
    val visited = mutableSetOf<String>()
    val queue = ArrayDeque<String>()
    queue.addAll(rawSchema.allOfParents)

    while (queue.isNotEmpty()) {
        val parentName = queue.removeFirst()
        if (!visited.add(parentName)) continue

        val parent = bySchemaName[parentName] ?: continue
        if (parent.rawSchema.discriminatorPropertyName != null) return parent

        queue.addAll(parent.rawSchema.allOfParents)
    }
    return null
}

private fun FieldDO.addAnnotation(a: ModelAnnotationDO): FieldDO {
    val exists = annotations.any { it.useSite == a.useSite && it.fqName == a.fqName && it.argsCode == a.argsCode }
    return if (exists) this else copy(annotations = annotations + a)
}
