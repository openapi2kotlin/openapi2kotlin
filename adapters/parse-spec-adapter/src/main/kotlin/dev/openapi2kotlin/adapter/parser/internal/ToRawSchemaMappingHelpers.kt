package dev.openapi2kotlin.adapter.parser.internal

import dev.openapi2kotlin.application.core.openapi2kotlin.domain.raw.RawSchemaDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.raw.RawSchemaDO.RawFieldTypeDO
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.ComposedSchema
import io.swagger.v3.oas.models.media.Schema

internal fun Map<String, Schema<*>>.collectUsedAsPropertyNames(): Set<String> =
    buildSet {
        values.forEach { ownerSchema ->
            ownerSchema.properties?.values?.forEach { propertySchema ->
                collectRefNamesFromSchema(propertySchema, this)
            }
        }
    }

internal fun OpenAPI.collectUsedInPathsNames(): Set<String> =
    buildSet {
        paths?.values?.forEach { pathItem ->
            pathItem.readOperations().forEach { operation ->
                operation.requestBody?.content?.values?.forEach { mediaType ->
                    collectRefNamesFromSchema(mediaType.schema, this)
                }
                operation.responses?.values?.forEach { apiResponse ->
                    apiResponse.content?.values?.forEach { mediaType ->
                        collectRefNamesFromSchema(mediaType.schema, this)
                    }
                }
                operation.parameters?.forEach { parameter ->
                    collectRefNamesFromSchema(parameter.schema, this)
                }
            }
        }
    }

internal fun Map.Entry<String, Schema<*>>.toRawSchema(
    usedAsPropertyNames: Set<String>,
    usedInPathsNames: Set<String>,
): RawSchemaDO {
    val name = key
    val schema = value
    val discriminatorMapping = schema.discriminator?.mapping.orEmpty()

    return RawSchemaDO(
        originalName = name,
        allOfParents = schema.refChildren(schema.allOf),
        oneOfChildren = schema.refChildren(schema.oneOf),
        enumValues = schema.enum?.map { it.toString() }.orEmpty(),
        isArraySchema = schema.type == "array",
        arrayItemType = schema.arrayItemType(),
        constraints = schemaToConstraints(schema),
        ownProperties = schema.collectOwnProperties(),
        discriminatorPropertyName = schema.discriminator?.propertyName,
        discriminatorMapping = discriminatorMapping,
        isDiscriminatorSelfMapped =
            discriminatorMapping.values.any { target ->
                target.substringAfterLast('/') == name
            },
        usedInPaths = name in usedInPathsNames,
        usedAsProperty = name in usedAsPropertyNames,
        description = schema.schemaDescription(),
    )
}

private fun Schema<*>.collectOwnProperties(): MutableMap<String, RawSchemaDO.SchemaPropertyDO> {
    val ownProps = mutableMapOf<String, RawSchemaDO.SchemaPropertyDO>()
    mergePropertiesInto(ownProps)
    allOf
        ?.filter { it.`$ref` == null }
        ?.forEach { inlineSchema -> inlineSchema.mergePropertiesInto(ownProps) }
    return ownProps
}

private fun Schema<*>.mergePropertiesInto(target: MutableMap<String, RawSchemaDO.SchemaPropertyDO>) {
    val requiredNames = required?.toSet().orEmpty()
    properties.orEmpty().forEach { (propName, propSchema) ->
        target.merge(
            propName,
            propSchema.toSchemaProperty(propName, propName in requiredNames),
            ::mergeSchemaProperty,
        )
    }
}

private fun Schema<*>.toSchemaProperty(
    propName: String,
    required: Boolean,
): RawSchemaDO.SchemaPropertyDO =
    RawSchemaDO.SchemaPropertyDO(
        name = propName,
        type = schemaToRawTypeForProperty(this, required),
        required = required,
        defaultValue = default?.toString(),
        description = description,
        constraints = schemaToConstraints(this),
    )

private fun Schema<*>.arrayItemType(): RawFieldTypeDO? =
    if (type == "array" && this is ArraySchema) {
        schemaToRawTypeForProperty(items, required = true)
    } else {
        null
    }

private fun Schema<*>.schemaDescription(): String? =
    description
        ?: (this as? ComposedSchema)?.allOf
            ?.firstOrNull { it.`$ref` == null && !it.description.isNullOrBlank() }
            ?.description

private fun Schema<*>.refChildren(children: List<Schema<*>>?): List<String> =
    children
        ?.mapNotNull { it.`$ref`?.substringAfterLast('/') }
        ?.distinct()
        .orEmpty()
