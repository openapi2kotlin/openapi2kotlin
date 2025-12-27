package dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.model.helpers

import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.*
import dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.common.renderDefault
import dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.common.toFinalType
import dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.common.withNullability
import dev.openapi2kotlin.application.usecase.openapi2kotlin.OpenApi2KotlinUseCase

internal fun List<ModelDO>.handleFields(
    cfg: OpenApi2KotlinUseCase.ModelConfig.MappingConfig,
) {
    val byName: Map<String, ModelDO> = associateBy { it.rawSchema.originalName }

    forEach { component ->
        // schema-level doc is part of the "final recipe" (generator must stay dumb)
        component.kdoc = component.rawSchema.description

        val parentPropSchemas = mutableMapOf<String, PropInfo>()
        component.rawSchema.allOfParents.forEach { parentName ->
            collectAllPropertiesFromSchema(
                schemaName = parentName,
                schemas = byName,
                into = parentPropSchemas,
                visited = mutableSetOf(),
                cfg = cfg,
            )
        }

        val fields = mutableListOf<FieldDO>()

        parentPropSchemas.forEach { (propName, info) ->
            fields += FieldDO(
                originalName = propName,
                generatedName = propName.toKotlinName(),
                overridden = true,
                type = info.type,
                required = info.required,
                defaultValueCode = info.defaultValueCode,
                kdoc = info.kdoc,
            )
        }

        val ownPropSchemas = component.rawSchema.ownProperties
            .mapValues { (_, prop) ->
                PropInfo(
                    type = prop.type.toFinalType(cfg),
                    required = prop.required,
                    defaultValueCode = prop.defaultValue?.let { renderDefault(prop.type, cfg, it) },
                    kdoc = prop.description,
                )
            }
            .toMutableMap()

        ownPropSchemas.forEach { (propName, info) ->
            val parentInfo = parentPropSchemas[propName]
            val parentRequired = parentInfo?.required ?: false
            val effectiveRequired = parentRequired || info.required

            val overridden = parentInfo != null

            // overridden field: if own doc missing, inherit doc from parent
            val effectiveKdoc =
                when {
                    !info.kdoc.isNullOrBlank() -> info.kdoc
                    overridden -> parentInfo?.kdoc
                    else -> null
                }

            val newField = FieldDO(
                originalName = propName,
                generatedName = propName.toKotlinName(),
                overridden = overridden,
                type = info.type.withNullability(nullable = !effectiveRequired),
                required = effectiveRequired,
                defaultValueCode = info.defaultValueCode,
                kdoc = effectiveKdoc,
            )

            val existingIndex = fields.indexOfFirst { it.originalName == propName }
            if (existingIndex >= 0) fields[existingIndex] = newField else fields += newField
        }

        component.rawSchema.discriminatorPropertyName?.let { discName ->
            val alreadyPresent = fields.any { it.originalName == discName }
            if (!alreadyPresent) {
                fields.add(
                    0,
                    FieldDO(
                        originalName = discName,
                        generatedName = discName.toKotlinName(),
                        overridden = false,
                        type = TrivialTypeDO(TrivialTypeDO.Kind.STRING, nullable = false),
                        required = true,
                        defaultValueCode = null,
                        kdoc = null,
                    ),
                )
            }
        }

        component.fields = fields.toMutableList()
    }

    val byNameAfter = associateBy { it.rawSchema.originalName }

    forEach { component ->
        val parentName = when (val shape = component.modelShape) {
            is ModelShapeDO.DataClass -> shape.extend
            is ModelShapeDO.OpenClass -> shape.extend
            else -> null
        } ?: return@forEach

        val parent = byNameAfter[parentName] ?: return@forEach

        component.fields = component.fields.map { field ->
            val parentField = parent.fields.firstOrNull { it.generatedName == field.generatedName }

            val parentRequired = parentField?.required ?: false
            val finalRequired = parentRequired || field.required
            val finalType = field.type.withNullability(nullable = !finalRequired)

            // requiredness pass must not rewrite docs; docs were resolved in the first pass
            field.copy(type = finalType, required = finalRequired)
        }.toMutableList()
    }
}

private data class PropInfo(
    val type: FieldTypeDO,
    val required: Boolean,
    val defaultValueCode: String?,
    val kdoc: String?,
)

private fun collectAllPropertiesFromSchema(
    schemaName: String,
    schemas: Map<String, ModelDO>,
    into: MutableMap<String, PropInfo>,
    visited: MutableSet<String>,
    cfg: OpenApi2KotlinUseCase.ModelConfig.MappingConfig,
) {
    if (!visited.add(schemaName)) return
    val schema = schemas[schemaName] ?: return

    schema.rawSchema.ownProperties.values.forEach { prop ->
        val info = PropInfo(
            type = prop.type.toFinalType(cfg),
            required = prop.required,
            defaultValueCode = prop.defaultValue?.let { renderDefault(prop.type, cfg, it) },
            kdoc = prop.description,
        )
        into.merge(prop.name, info) { a, b ->
            PropInfo(
                type = a.type,
                required = a.required || b.required,
                defaultValueCode = a.defaultValueCode ?: b.defaultValueCode,
                // retain any existing doc; if absent, take the incoming one
                kdoc = a.kdoc ?: b.kdoc,
            )
        }
    }

    schema.rawSchema.allOfParents.forEach { parentName ->
        collectAllPropertiesFromSchema(parentName, schemas, into, visited, cfg)
    }
}