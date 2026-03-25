package dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.model.helpers

import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.FieldDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.ListTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.ModelAnnotationDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.ModelDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.ModelShapeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.PolymorphismDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.RefTypeDO
import dev.openapi2kotlin.application.usecase.openapi2kotlin.OpenApi2KotlinUseCase

internal const val JSON_PROPERTY = "com.fasterxml.jackson.annotation.JsonProperty"
internal const val JSON_TYPE_INFO = "com.fasterxml.jackson.annotation.JsonTypeInfo"
internal const val JSON_SUB_TYPES = "com.fasterxml.jackson.annotation.JsonSubTypes"
internal const val JSON_IGNORE_PROPERTIES = "com.fasterxml.jackson.annotation.JsonIgnoreProperties"
internal const val JSON_VALUE = "com.fasterxml.jackson.annotation.JsonValue"
internal const val JSON_CREATOR = "com.fasterxml.jackson.annotation.JsonCreator"

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
internal fun List<ModelDO>.handleJacksonAnnotations(cfg: OpenApi2KotlinUseCase.ModelConfig) {
    if (cfg.serialization != OpenApi2KotlinUseCase.ModelConfig.Serialization.JACKSON) return

    val bySchemaName: Map<String, ModelDO> = associateBy { it.rawSchema.originalName }
    applyEnumJacksonAnnotations(cfg)
    applyJsonPropertyMappings(cfg)
    val wrappers = buildPolymorphismAnnotations(cfg, bySchemaName)
    applyOneOfWrapperFieldAnnotations(wrappers, bySchemaName)
    applyReadOnlyDiscriminators(bySchemaName)
}

private fun List<ModelDO>.applyEnumJacksonAnnotations(cfg: OpenApi2KotlinUseCase.ModelConfig) {
    if (!cfg.jacksonJsonValue && !cfg.jacksonJsonCreator) return

    forEach { model ->
        if (model.modelShape !is ModelShapeDO.EnumClass) return@forEach
        if (cfg.jacksonJsonValue) {
            model.enumValueAnnotations +=
                ModelAnnotationDO(
                    useSite = ModelAnnotationDO.UseSiteDO.GET,
                    fqName = JSON_VALUE,
                )
        }
        if (cfg.jacksonJsonCreator) model.enumFromValueAnnotations += ModelAnnotationDO(fqName = JSON_CREATOR)
    }
}

private fun List<ModelDO>.applyJsonPropertyMappings(cfg: OpenApi2KotlinUseCase.ModelConfig) {
    if (!cfg.jacksonJsonPropertyMapping) return

    forEach { model ->
        val useSite = model.jsonPropertyUseSite()
        model.fields =
            model.fields.map { field ->
                if (field.originalName == field.generatedName) {
                    field
                } else {
                    field.addAnnotation(
                        ModelAnnotationDO(
                            useSite = useSite,
                            fqName = JSON_PROPERTY,
                            argsCode = listOf("\"${field.originalName}\""),
                        ),
                    )
                }
            }.toMutableList()
    }
}

private fun List<ModelDO>.buildPolymorphismAnnotations(
    cfg: OpenApi2KotlinUseCase.ModelConfig,
    bySchemaName: Map<String, ModelDO>,
): Map<String, PolymorphismDO> {
    val wrappers = mutableMapOf<String, PolymorphismDO>()
    forEach { parent ->
        val metadata = parent.buildPolymorphismMetadata() ?: return@forEach
        parent.polymorphism = metadata.polymorphism

        if (metadata.isOneOfWrapperSealedInterface) {
            wrappers[parent.rawSchema.originalName] = metadata.polymorphism
        } else {
            parent.annotations += parent.buildPolymorphismAnnotations(metadata, bySchemaName, cfg)
        }
    }
    return wrappers
}

private fun List<ModelDO>.applyOneOfWrapperFieldAnnotations(
    oneOfWrapperPolymorphismBySchemaName: Map<String, PolymorphismDO>,
    bySchemaName: Map<String, ModelDO>,
) {
    if (oneOfWrapperPolymorphismBySchemaName.isEmpty()) return

    forEach { owner ->
        owner.fields =
            owner.fields.map { field ->
                val wrapperSchemaName =
                    field.findWrapperSchemaNameReferenced(oneOfWrapperPolymorphismBySchemaName.keys)
                        ?: return@map field
                val wrapperPolymorphism =
                    oneOfWrapperPolymorphismBySchemaName[wrapperSchemaName] ?: return@map field
                val subtypeEntries =
                    buildWrapperSubtypeEntries(
                        wrapperSchemaName = wrapperSchemaName,
                        wrapperPolymorphism = wrapperPolymorphism,
                        bySchemaName = bySchemaName,
                    )
                if (subtypeEntries.isEmpty()) return@map field

                field
                    .addAnnotation(
                        ModelAnnotationDO(
                            useSite = ModelAnnotationDO.UseSiteDO.FIELD,
                            fqName = JSON_TYPE_INFO,
                            argsCode =
                                listOf(
                                    "use = JsonTypeInfo.Id.NAME",
                                    "include = JsonTypeInfo.As.PROPERTY",
                                    "property = \"${wrapperPolymorphism.discriminatorPropertyOriginalName}\"",
                                    "visible = true",
                                ),
                        ),
                    )
                    .addAnnotation(
                        ModelAnnotationDO(
                            useSite = ModelAnnotationDO.UseSiteDO.FIELD,
                            fqName = JSON_SUB_TYPES,
                            argsCode = subtypeEntries,
                        ),
                    )
            }.toMutableList()
    }
}

private fun List<ModelDO>.applyReadOnlyDiscriminators(bySchemaName: Map<String, ModelDO>) {
    forEach { model ->
        if (!model.shouldAddReadOnlyDiscriminator()) return@forEach
        val discriminatorName = model.resolveDiscriminatorName(bySchemaName) ?: return@forEach
        model.fields = model.fields.map { it.withReadOnlyDiscriminator(discriminatorName) }.toMutableList()
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

        is ListTypeDO ->
            when (val e = t.elementType) {
                is RefTypeDO -> e.schemaName.takeIf { it in wrapperSchemaNames }
                else -> null
            }

        else -> null
    }
}

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

internal fun FieldDO.addAnnotation(a: ModelAnnotationDO): FieldDO {
    val exists =
        annotations.any {
            it.useSite == a.useSite &&
                it.fqName == a.fqName &&
                it.argsCode == a.argsCode
        }

    return if (exists) this else copy(annotations = annotations + a)
}
