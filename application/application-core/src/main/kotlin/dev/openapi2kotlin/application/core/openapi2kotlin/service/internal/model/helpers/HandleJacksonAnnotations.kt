package dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.model.helpers

import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.FieldDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.ListTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.ModelAnnotationDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.ModelDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.ModelShapeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.PolymorphismDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.RefTypeDO
import dev.openapi2kotlin.application.usecase.openapi2kotlin.OpenApi2KotlinUseCase

private const val JSON_PROPERTY = "com.fasterxml.jackson.annotation.JsonProperty"
private const val JSON_TYPE_INFO = "com.fasterxml.jackson.annotation.JsonTypeInfo"
private const val JSON_SUB_TYPES = "com.fasterxml.jackson.annotation.JsonSubTypes"
private const val JSON_IGNORE_PROPERTIES = "com.fasterxml.jackson.annotation.JsonIgnoreProperties"

/**
 * Applies Jackson-related annotations to generated models.
 *
 * Responsibilities:
 *  - property name mapping (@JsonProperty)
 *  - default null values for optional fields
 *  - polymorphic type handling (@JsonTypeInfo, @JsonSubTypes)
 *  - discriminator defaults for concrete leaf types
 *  - discriminator read-only enforcement on leaves
 *
 * All structural decisions (sealed / open / data class) must already be resolved
 * in ModelDO.modelShape prior to this step.
 */
internal fun List<ModelDO>.handleJacksonAnnotations(
    cfg: OpenApi2KotlinUseCase.ModelConfig.JacksonConfig,
) {
    if (!cfg.enabled) return

    val bySchemaName: Map<String, ModelDO> = associateBy { it.rawSchema.originalName }

    /* ---------------------------------------------------------------------
     * 1) @JsonProperty for renamed fields
     * ------------------------------------------------------------------- */

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

    /* ---------------------------------------------------------------------
     * 2) Default null values for optional nullable fields
     * ------------------------------------------------------------------- */

    forEach { model ->
        model.fields = model.fields
            .map { f ->
                when {
                    f.defaultValueCode != null -> f
                    !f.required && f.type.nullable -> f.copy(defaultValueCode = "null")
                    else -> f
                }
            }
            .toMutableList()
    }

    /* ---------------------------------------------------------------------
     * 3) Polymorphism metadata + annotations
     *
     * Principles:
     *  - For oneOf-wrapper sealed interfaces ("RefOrValue" unions):
     *      * compute polymorphism metadata
     *      * DO NOT add @JsonTypeInfo/@JsonSubTypes on the interface
     *      * later annotate the FIELDS that reference the wrapper
     *
     *  - For "real" polymorphic bases (open class / inheritance base):
     *      * keep class-level @JsonTypeInfo/@JsonSubTypes
     * ------------------------------------------------------------------- */

    // wrapper schema name -> polymorphism metadata (for field-site annotation)
    val oneOfWrapperPolymorphismBySchemaName = mutableMapOf<String, PolymorphismDO>()

    forEach { parent ->
        val discOriginal = parent.rawSchema.discriminatorPropertyName ?: return@forEach
        val discGenerated = discOriginal.toKotlinName()

        // OpenAPI discriminator mapping: discriminatorValue -> schemaRef
        val mappingValueToSchemaName =
            parent.rawSchema.discriminatorMapping
                .mapValues { (_, ref) -> ref.substringAfterLast('/') }

        val hasMappingToOthers =
            mappingValueToSchemaName.values.any { it != parent.rawSchema.originalName }

        val isOneOfRoot = parent.rawSchema.oneOfChildren.isNotEmpty()
        val isAllOfRoot = parent.allOfChildren.isNotEmpty()

        // Determine polymorphic subtypes (raw schema names)
        val childrenSchemaNames: List<String> = when {
            isOneOfRoot ->
                parent.rawSchema.oneOfChildren

            hasMappingToOthers ->
                mappingValueToSchemaName.values
                    .distinct()
                    .filter { it != parent.rawSchema.originalName }

            isAllOfRoot ->
                parent.allOfChildren

            else ->
                emptyList()
        }

        // Not a polymorphic root → do not emit polymorphism annotations
        if (childrenSchemaNames.isEmpty()) return@forEach

        val schemaNameToDiscriminatorValue =
            parent.buildSchemaNameToDiscriminatorValue(
                children = childrenSchemaNames,
                mappingValueToSchemaName = mappingValueToSchemaName,
            )

        parent.polymorphism = PolymorphismDO(
            discriminatorPropertyOriginalName = discOriginal,
            discriminatorPropertyGeneratedName = discGenerated,
            schemaNameToDiscriminatorValue = schemaNameToDiscriminatorValue,
        )

        // --- treat sealed interface + oneOf as "wrapper" union ---
        val isOneOfWrapperSealedInterface =
            isOneOfRoot && parent.modelShape is ModelShapeDO.SealedInterface

        if (isOneOfWrapperSealedInterface) {
            // Store for field-site annotation and skip type-level @JsonTypeInfo/@JsonSubTypes
            oneOfWrapperPolymorphismBySchemaName[parent.rawSchema.originalName] = parent.polymorphism!!
            return@forEach
        }

        // Otherwise: annotate the type itself (open-class base / allOf base / etc.)
        val isConcreteParent =
            parent.modelShape is ModelShapeDO.OpenClass ||
                    parent.modelShape is ModelShapeDO.DataClass

        // Include the parent itself as a subtype only if:
        //  - discriminator mapping explicitly points to itself
        //  - the parent is instantiable in Kotlin
        val includeSelfSubtype =
            parent.rawSchema.isDiscriminatorSelfMapped && isConcreteParent

        val subtypeEntries = buildList {
            if (includeSelfSubtype) {
                val selfDiscValue =
                    schemaNameToDiscriminatorValue[parent.rawSchema.originalName]
                        ?: parent.rawSchema.originalName

                add(
                    "JsonSubTypes.Type(value = ${parent.generatedName}::class, name = \"$selfDiscValue\")"
                )
            }

            childrenSchemaNames.forEach { childSchemaName ->
                val child = bySchemaName[childSchemaName] ?: return@forEach
                val discValue =
                    schemaNameToDiscriminatorValue[childSchemaName] ?: childSchemaName

                add(
                    "JsonSubTypes.Type(value = ${child.generatedName}::class, name = \"$discValue\")"
                )
            }
        }

        val annotations = buildList {
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

            add(
                ModelAnnotationDO(
                    fqName = JSON_SUB_TYPES,
                    argsCode = subtypeEntries,
                )
            )
        }

        parent.annotations = parent.annotations + annotations
    }

    /* ---------------------------------------------------------------------
     * 3.5) Field-site polymorphism for oneOf wrapper unions
     *
     * Add @field:JsonTypeInfo + @field:JsonSubTypes on any field whose type
     * is the wrapper schema (or list of wrapper schema).
     * ------------------------------------------------------------------- */

    if (oneOfWrapperPolymorphismBySchemaName.isNotEmpty()) {
        forEach { owner ->
            owner.fields = owner.fields
                .map { f ->
                    val wrapperSchemaName = f.findWrapperSchemaNameReferenced(oneOfWrapperPolymorphismBySchemaName.keys)
                        ?: return@map f

                    val wrapperPolymorphism = oneOfWrapperPolymorphismBySchemaName[wrapperSchemaName]
                        ?: return@map f

                    val subtypeEntries = buildList {
                        wrapperPolymorphism.schemaNameToDiscriminatorValue
                            .filterKeys { it != wrapperSchemaName } // wrapper itself isn't a concrete subtype
                            .forEach { (childSchemaName, discValue) ->
                                val child = bySchemaName[childSchemaName] ?: return@forEach
                                add("JsonSubTypes.Type(value = ${child.generatedName}::class, name = \"$discValue\")")
                            }
                    }

                    // If we could not resolve children (edge cases), do not annotate.
                    if (subtypeEntries.isEmpty()) return@map f

                    f.addAnnotation(
                        ModelAnnotationDO(
                            useSite = ModelAnnotationDO.UseSiteDO.FIELD,
                            fqName = JSON_TYPE_INFO,
                            argsCode = listOf(
                                "use = JsonTypeInfo.Id.NAME",
                                "include = JsonTypeInfo.As.PROPERTY",
                                "property = \"${wrapperPolymorphism.discriminatorPropertyOriginalName}\"",
                                "visible = true",
                            ),
                        )
                    ).addAnnotation(
                        ModelAnnotationDO(
                            useSite = ModelAnnotationDO.UseSiteDO.FIELD,
                            fqName = JSON_SUB_TYPES,
                            argsCode = subtypeEntries,
                        )
                    )
                }
                .toMutableList()
        }
    }

    /* ---------------------------------------------------------------------
     * 4) Default discriminator value on concrete instantiable types
     *
     * Ensures stable serialization without requiring callers to explicitly
     * provide @type for instantiable instances (including intermediate allOf bases).
     * ------------------------------------------------------------------- */

    if (cfg.defaultDiscriminatorValue) {
        forEach { model ->
            val isConcrete =
                model.modelShape is ModelShapeDO.DataClass ||
                        model.modelShape is ModelShapeDO.OpenClass

            if (!isConcrete) return@forEach

            val parentWithDisc = model.findNearestDiscriminatorParent(bySchemaName)
            val discOriginal =
                model.rawSchema.discriminatorPropertyName
                    ?: parentWithDisc?.rawSchema?.discriminatorPropertyName
                    ?: return@forEach

            val discValue =
                // 1) Prefer this schema's own discriminator mapping key (TMF-style)
                model.selfDiscriminatorValueFromOwnMapping()
                // 2) Otherwise, inherit from nearest discriminator parent mapping
                    ?: parentWithDisc?.polymorphism
                        ?.schemaNameToDiscriminatorValue
                        ?.get(model.rawSchema.originalName)
                    // 3) Last resort fallback
                    ?: model.rawSchema.originalName

            model.fields = model.fields.map { f ->
                if (f.originalName == discOriginal && f.defaultValueCode == null)
                    f.copy(defaultValueCode = "\"$discValue\"")
                else f
            }.toMutableList()
        }
    }

    /* ---------------------------------------------------------------------
     * 5) Make discriminator READ_ONLY on concrete non-polymorphic leaves
     *
     * Prevents accidental acceptance of incorrect @type values.
     * ------------------------------------------------------------------- */

    forEach { model ->
        val isConcrete =
            model.modelShape is ModelShapeDO.DataClass ||
                    model.modelShape is ModelShapeDO.OpenClass

        if (!isConcrete) return@forEach
        if (model.rawSchema.oneOfChildren.isNotEmpty()) return@forEach
        if (model.allOfChildren.isNotEmpty()) return@forEach

        val parentWithDisc = model.findNearestDiscriminatorParent(bySchemaName)
        val discOriginal =
            model.rawSchema.discriminatorPropertyName
                ?: parentWithDisc?.rawSchema?.discriminatorPropertyName
                ?: return@forEach

        model.fields = model.fields.map { f ->
            if (f.originalName != discOriginal) f
            else f.addAnnotation(
                ModelAnnotationDO(
                    useSite = ModelAnnotationDO.UseSiteDO.GET,
                    fqName = JSON_PROPERTY,
                    argsCode = listOf(
                        "value = \"$discOriginal\"",
                        "access = JsonProperty.Access.READ_ONLY",
                    ),
                )
            )
        }.toMutableList()
    }
}

/**
 * If this field references any of the oneOf wrapper schema names (directly or as list element),
 * return the wrapper schema name, else null.
 */
private fun FieldDO.findWrapperSchemaNameReferenced(wrapperSchemaNames: Set<String>): String? {
    return when (val t = type) {
        is RefTypeDO ->
            t.schemaName.takeIf { it in wrapperSchemaNames }

        is ListTypeDO -> when (val e = t.elementType) {
            is RefTypeDO -> e.schemaName.takeIf { it in wrapperSchemaNames }
            else -> null
        }

        else -> null
    }
}


/* =====================================================================
 * Helper functions
 * =================================================================== */

/**
 * Determines the appropriate @JsonProperty use-site.
 *
 * Interfaces require @get:, concrete classes prefer @param:.
 */
private fun ModelDO.jsonPropertyUseSite(): ModelAnnotationDO.UseSiteDO =
    when (modelShape) {
        is ModelShapeDO.SealedInterface -> ModelAnnotationDO.UseSiteDO.GET
        else -> ModelAnnotationDO.UseSiteDO.PARAM
    }

/**
 * Builds schemaName -> discriminatorValue mapping for a polymorphic root.
 *
 * Uses explicit OpenAPI mapping when present, otherwise defaults to schema name.
 */
private fun ModelDO.buildSchemaNameToDiscriminatorValue(
    children: List<String>,
    mappingValueToSchemaName: Map<String, String>,
): Map<String, String> {
    val inverted =
        mappingValueToSchemaName.entries.associate { (discValue, schemaName) ->
            schemaName to discValue
        }

    return buildMap {
        put(rawSchema.originalName, inverted[rawSchema.originalName] ?: rawSchema.originalName)
        for (schemaName in children) {
            put(schemaName, inverted[schemaName] ?: schemaName)
        }
    }
}

/**
 * Walks up allOf parents to find the nearest schema declaring a discriminator.
 */
private fun ModelDO.findNearestDiscriminatorParent(
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

private fun FieldDO.addAnnotation(a: ModelAnnotationDO): FieldDO {
    val exists =
        annotations.any {
            it.useSite == a.useSite &&
                    it.fqName == a.fqName &&
                    it.argsCode == a.argsCode
        }

    return if (exists) this else copy(annotations = annotations + a)
}

/**
 * If this model declares a discriminator mapping that points to itself,
 * return the discriminator VALUE (mapping key) that should be used in JSON "@type".
 *
 * Example:
 *   mapping:
 *     ProductOfferingPrice: '#/components/schemas/ProductOfferingPrice_FVO'
 * -> returns "ProductOfferingPrice"
 */
private fun ModelDO.selfDiscriminatorValueFromOwnMapping(): String? {
    val ownName = rawSchema.originalName
    // discriminatorMapping: Map<discValue, ref>
    return rawSchema.discriminatorMapping
        .entries
        .firstOrNull { (_, ref) ->
            ref.substringAfterLast('/') == ownName
        }
        ?.key
}