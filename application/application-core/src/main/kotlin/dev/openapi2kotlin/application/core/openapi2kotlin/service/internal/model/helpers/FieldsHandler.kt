package dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.model.helpers

import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.FieldDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.ModelShapeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.ListTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.PrimitiveTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.RefTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.ModelDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.FieldTypeDO


/**
 * Fourth pass – decide fields and their types, and which ones are overridden.
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
internal fun fieldsHandler(
    schemas: List<ModelDO>,
) {
    val byName: Map<String, ModelDO> =schemas.associateBy { it.rawSchema.originalName }

    // ----- FIRST PASS: build fields for each component -----
   schemas.forEach { component ->

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
                generatedName = toKotlinName(propName),
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
                generatedName = toKotlinName(propName),
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
                val generatedName = toKotlinName(discName)
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
    // If parent says "required", child must also be required (non-null override).
    val byNameAfter =schemas.associateBy { it.rawSchema.originalName }

   schemas.forEach { component ->
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

/**
 * Recursively collect *all* properties for a schema, following its allOf chain.
 * Required flags are merged with OR semantics.
 */
private fun collectAllPropertiesFromSchema(
    schemaName: String,
    schemas: Map<String, ModelDO>,
    into: MutableMap<String, PropInfo>,
    visited: MutableSet<String>,
) {
    if (!visited.add(schemaName)) return

    val schema = schemas[schemaName] ?: return

    // own properties
    schema.rawSchema.ownProperties.values.forEach { prop ->
        val info = PropInfo(prop.type, prop.required)
        into.merge(prop.name, info) { a, b ->
            PropInfo(
                type = a.type,            // type should match; keep first
                required = a.required || b.required,
            )
        }
    }

    // walk its allOf chain
    schema.rawSchema.allOfParents.forEach { parentName ->
        collectAllPropertiesFromSchema(parentName, schemas, into, visited)
    }
}

/**
 * Convert OpenAPI property name to Kotlin identifier.
 *
 * Currently:
 *  - "@type" → "atType"
 *  - otherwise returns the original name (you can extend later if needed).
 */
private fun toKotlinName(original: String): String =
    if (original.startsWith("@") && original.length > 1) {
        val rest = original.substring(1)
        "at" + rest.replaceFirstChar { it.uppercaseChar() }
    } else {
        original
    }

/**
 * Adjust nullability of our internal DtoType.
 */
private fun FieldTypeDO.withNullability(nullable: Boolean): FieldTypeDO = when (this) {
    is PrimitiveTypeDO -> copy(nullable = nullable)
    is RefTypeDO -> copy(nullable = nullable)
    is ListTypeDO -> copy(nullable = nullable)
}
