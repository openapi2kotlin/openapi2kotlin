package dev.openapi2kotlin.adapter.parser.internal

import dev.openapi2kotlin.application.core.openapi2kotlin.model.raw.RawSchemaDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.raw.RawSchemaDO.RawArrayTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.raw.RawSchemaDO.RawFieldTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.raw.RawSchemaDO.RawPrimitiveTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.raw.RawSchemaDO.RawRefTypeDO
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.ComposedSchema
import io.swagger.v3.oas.models.media.Schema

internal fun OpenAPI.toRawSchemas(): List<RawSchemaDO> {
    val schemas: Map<String, Schema<*>> = components?.schemas.orEmpty()

    // --- detect "usedAsProperty" ---
    val usedAsPropertyNames = mutableSetOf<String>()
    schemas.values.forEach { ownerSchema ->
        ownerSchema.properties?.values?.forEach { propertySchema ->
            collectRefNamesFromSchema(propertySchema, usedAsPropertyNames)
        }
    }

    // --- detect "usedInPaths" ---
    val usedInPathsNames = mutableSetOf<String>()
    paths?.values?.forEach { pathItem ->
        pathItem.readOperations().forEach { operation ->
            operation.requestBody?.content?.values?.forEach { mediaType ->
                collectRefNamesFromSchema(mediaType.schema, usedInPathsNames)
            }
            operation.responses?.values?.forEach { apiResponse ->
                apiResponse.content?.values?.forEach { mediaType ->
                    collectRefNamesFromSchema(mediaType.schema, usedInPathsNames)
                }
            }
            operation.parameters?.forEach { parameter ->
                collectRefNamesFromSchema(parameter.schema, usedInPathsNames)
            }
        }
    }

    return schemas.entries.map { (name, schema) ->
        val allOfParents: List<String> =
            schema.allOf
                ?.mapNotNull { it.`$ref`?.substringAfterLast('/') }
                ?.distinct()
                ?: emptyList()

        val oneOfChildren: List<String> =
            schema.oneOf
                ?.mapNotNull { it.`$ref`?.substringAfterLast('/') }
                ?.distinct()
                ?: emptyList()

        val enumValues: List<String> =
            schema.enum?.map { it.toString() } ?: emptyList()

        val isArraySchema = schema.type == "array"
        val arrayItemType: RawFieldTypeDO? =
            if (isArraySchema && schema is ArraySchema) {
                // element type for the "typealias X = List<Elem>"
                schemaToRawTypeForProperty(
                    schema = schema.items,
                    required = true,
                )
            } else null

        val ownProps = mutableMapOf<String, RawSchemaDO.SchemaPropertyDO>()

        // schema doc
        val schemaDescription: String? =
            schema.description
                ?: (schema as? ComposedSchema)?.allOf
                    ?.firstOrNull { it.`$ref` == null && !it.description.isNullOrBlank() }
                    ?.description

        // top-level properties
        val requiredTop = schema.required?.toSet().orEmpty()
        schema.properties.orEmpty().forEach { (propName, propSchema) ->
            val required = propName in requiredTop
            val rawType = schemaToRawTypeForProperty(propSchema, required)
            val defaultValue = propSchema.default?.toString()
            ownProps.merge(
                propName,
                RawSchemaDO.SchemaPropertyDO(
                    name = propName,
                    type = rawType,
                    required = required,
                    defaultValue = defaultValue,
                    description = propSchema.description,
                ),
                ::mergeSchemaProperty,
            )
        }

        // inline allOf parts
        schema.allOf
            ?.filter { it.`$ref` == null }
            ?.forEach { inlineSchema ->
                val requiredInline = inlineSchema.required?.toSet().orEmpty()
                inlineSchema.properties.orEmpty().forEach { (propName, propSchema) ->
                    val required = propName in requiredInline
                    val rawType = schemaToRawTypeForProperty(propSchema, required)
                    val defaultValue = propSchema.default?.toString()
                    ownProps.merge(
                        propName,
                        RawSchemaDO.SchemaPropertyDO(
                            name = propName,
                            type = rawType,
                            required = required,
                            defaultValue = defaultValue,
                            description = propSchema.description,
                        ),
                        ::mergeSchemaProperty,
                    )
                }
            }

        val discriminatorPropertyName = schema.discriminator?.propertyName

        val discriminatorMapping: Map<String, String> =
            schema.discriminator?.mapping.orEmpty()

        val isDiscriminatorSelfMapped: Boolean =
            discriminatorMapping.values.any { target ->
                target.substringAfterLast('/') == name
            }

        RawSchemaDO(
            originalName = name,
            allOfParents = allOfParents,
            oneOfChildren = oneOfChildren,
            enumValues = enumValues,
            isArraySchema = isArraySchema,
            arrayItemType = arrayItemType,
            ownProperties = ownProps,
            discriminatorPropertyName = discriminatorPropertyName,
            discriminatorMapping = discriminatorMapping,
            isDiscriminatorSelfMapped = isDiscriminatorSelfMapped,
            usedInPaths = name in usedInPathsNames,
            usedAsProperty = name in usedAsPropertyNames,
            description = schemaDescription,
        )
    }.sortedBy { it.originalName }
}

private fun schemaToRawTypeForProperty(
    schema: Schema<*>?,
    required: Boolean,
): RawFieldTypeDO {
    if (schema == null) {
        return RawPrimitiveTypeDO(RawPrimitiveTypeDO.Type.OBJECT, format = null, nullable = true)
    }

    val nullableFromRequired = !required
    val nullable = schema.nullable == true || nullableFromRequired

    if (schema is ArraySchema) {
        val elementType = schemaToRawTypeForProperty(schema.items, required = true)
        return RawArrayTypeDO(elementType = elementType, nullable = nullable)
    }

    schema.`$ref`?.let { ref ->
        val name = ref.substringAfterLast('/')
        return RawRefTypeDO(schemaName = name, nullable = nullable)
    }

    val t = when (schema.type) {
        "string" -> RawPrimitiveTypeDO.Type.STRING
        "number" -> RawPrimitiveTypeDO.Type.NUMBER
        "integer" -> RawPrimitiveTypeDO.Type.INTEGER
        "boolean" -> RawPrimitiveTypeDO.Type.BOOLEAN
        "object" -> RawPrimitiveTypeDO.Type.OBJECT
        else -> RawPrimitiveTypeDO.Type.OBJECT
    }

    return RawPrimitiveTypeDO(
        type = t,
        format = schema.format,
        nullable = nullable,
    )
}

private fun collectRefNamesFromSchema(schema: Schema<*>?, into: MutableSet<String>) {
    if (schema == null) return

    schema.`$ref`?.substringAfterLast('/')?.let(into::add)

    when (schema) {
        is ArraySchema -> {
            collectRefNamesFromSchema(schema.items, into)
        }

        is ComposedSchema -> {
            schema.allOf?.forEach { collectRefNamesFromSchema(it, into) }
            schema.oneOf?.forEach { collectRefNamesFromSchema(it, into) }
            schema.anyOf?.forEach { collectRefNamesFromSchema(it, into) }
        }
    }

    schema.properties?.values?.forEach { collectRefNamesFromSchema(it, into) }

    val additional = schema.additionalProperties
    if (additional is Schema<*>) {
        collectRefNamesFromSchema(additional, into)
    }
}

private fun mergeSchemaProperty(
    a: RawSchemaDO.SchemaPropertyDO,
    b: RawSchemaDO.SchemaPropertyDO,
): RawSchemaDO.SchemaPropertyDO =
    RawSchemaDO.SchemaPropertyDO(
        name = a.name,
        type = a.type,
        required = a.required || b.required,
        defaultValue = a.defaultValue ?: b.defaultValue,
        description = a.description ?: b.description,
    )
