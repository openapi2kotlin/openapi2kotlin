package dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.model.helpers

import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.*

/**
 * Decide fields and their types, and which ones are overridden.
 *
 * For each component:
 *  - parentProps = all properties from allOf parents (recursively, via SchemaComponent.ownProperties)
 *  - ownProps = ownProperties
 *  - fields = parent props + own props (own wins on name clash)
 *  - overridden:
 *      - parent props: always true
 *      - own props: true if also present in parentProps
 *
 *  Required → non-null:
 *      - if property name is not required in any definition, it becomes nullable.
 *
 *  Discriminator:
 *      - if discriminatorPropertyName exists
 *      - and no field with that originalName exists yet
 *      - add synthetic String field for it (e.g. "@type" → "atType").
 */
internal fun List<ModelDO>.handleFields() {
    val byName: Map<String, ModelDO> = associateBy { it.rawSchema.originalName }

    // ----- FIRST PASS: build fields for each component -----
    forEach { component ->

        // ----- parent properties (from allOf parents, recursively) -----
        val parentPropSchemas = mutableMapOf<String, PropInfo>()
        component.rawSchema.allOfParents.forEach { parentName ->
            collectAllPropertiesFromSchema(
                schemaName = parentName,
                schemas = byName,
                into = parentPropSchemas,
                visited = mutableSetOf(),
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
            )
        }

        // ----- own properties: already flattened top-level + inline allOf -----
        val ownPropSchemas = component.rawSchema.ownProperties
            .mapValues { (_, prop) -> PropInfo(prop.type, prop.required) }
            .toMutableMap()

        ownPropSchemas.forEach { (propName, info) ->
            val parentRequired = parentPropSchemas[propName]?.required ?: false
            val effectiveRequired = parentRequired || info.required

            val overridden = parentPropSchemas.containsKey(propName)

            val newField = FieldDO(
                originalName = propName,
                generatedName = propName.toKotlinName(),
                overridden = overridden,
                type = info.type.withNullability(nullable = !effectiveRequired),
                required = effectiveRequired,
            )

            val existingIndex = fields.indexOfFirst { it.originalName == propName }
            if (existingIndex >= 0) {
                // own definition wins, but effectiveRequired keeps parent’s requirement
                fields[existingIndex] = newField
            } else {
                fields += newField
            }
        }

        // ----- discriminator field (e.g. @type) if missing -----
        component.rawSchema.discriminatorPropertyName?.let { discName ->
            val alreadyPresent = fields.any { it.originalName == discName }
            if (!alreadyPresent) {
                val generatedName = discName.toKotlinName()
                fields.add(
                    0,
                    FieldDO(
                        originalName = discName,
                        generatedName = generatedName,
                        overridden = false,
                        type = PrimitiveTypeDO(PrimitiveTypeDO.PrimitiveTypeNameDO.STRING, nullable = false),
                        required = true,
                    ),
                )
            }
        }

        component.fields = fields.toMutableList()
    }

    // ----- SECOND PASS: enforce requiredness inheritance -----
    val byNameAfter = associateBy { it.rawSchema.originalName }

    forEach { component ->
        val parentName = when (val shape = component.modelShape) {
            is ModelShapeDO.DataClass -> shape.extend
            is ModelShapeDO.OpenClass -> shape.extend
            else -> null
        } ?: return@forEach

        val parent = byNameAfter[parentName] ?: return@forEach

        val adjustedFields = component.fields.map { field ->
            val parentField = parent.fields.firstOrNull { it.generatedName == field.generatedName }

            val parentRequired = parentField?.required ?: false
            val finalRequired = parentRequired || field.required

            val finalType = field.type.withNullability(nullable = !finalRequired)

            field.copy(
                type = finalType,
                required = finalRequired,
            )
        }.toMutableList()

        component.fields = adjustedFields
    }
}

/* ---------- helpers ---------- */

private data class PropInfo(
    val type: FieldTypeDO,
    val required: Boolean,
)

private fun collectAllPropertiesFromSchema(
    schemaName: String,
    schemas: Map<String, ModelDO>,
    into: MutableMap<String, PropInfo>,
    visited: MutableSet<String>,
) {
    if (!visited.add(schemaName)) return

    val schema = schemas[schemaName] ?: return

    schema.rawSchema.ownProperties.values.forEach { prop ->
        val info = PropInfo(prop.type, prop.required)
        into.merge(prop.name, info) { a, b ->
            PropInfo(
                type = a.type,
                required = a.required || b.required,
            )
        }
    }

    schema.rawSchema.allOfParents.forEach { parentName ->
        collectAllPropertiesFromSchema(parentName, schemas, into, visited)
    }
}
