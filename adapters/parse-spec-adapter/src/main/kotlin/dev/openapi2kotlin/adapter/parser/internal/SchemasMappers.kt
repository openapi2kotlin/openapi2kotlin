package dev.openapi2kotlin.adapter.parser.internal

import dev.openapi2kotlin.application.core.openapi2kotlin.model.raw.RawSchemaDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.raw.RawSchemaDO.ConstraintsDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.raw.RawSchemaDO.RawArrayTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.raw.RawSchemaDO.RawFieldTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.raw.RawSchemaDO.RawPrimitiveTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.raw.RawSchemaDO.RawRefTypeDO
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.ComposedSchema
import io.swagger.v3.oas.models.media.Schema
import java.math.BigDecimal

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

        // schema-level constraints
        val schemaConstraints: ConstraintsDO = schemaToConstraints(schema)

        // top-level properties
        val requiredTop = schema.required?.toSet().orEmpty()
        schema.properties.orEmpty().forEach { (propName, propSchema) ->
            val required = propName in requiredTop
            val rawType = schemaToRawTypeForProperty(propSchema, required)
            val defaultValue = propSchema.default?.toString()
            val constraints = schemaToConstraints(propSchema)
            ownProps.merge(
                propName,
                RawSchemaDO.SchemaPropertyDO(
                    name = propName,
                    type = rawType,
                    required = required,
                    defaultValue = defaultValue,
                    description = propSchema.description,
                    constraints = constraints,
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
                    val constraints = schemaToConstraints(propSchema)
                    ownProps.merge(
                        propName,
                        RawSchemaDO.SchemaPropertyDO(
                            name = propName,
                            type = rawType,
                            required = required,
                            defaultValue = defaultValue,
                            description = propSchema.description,
                            constraints = constraints,
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
            constraints = schemaConstraints,
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
        val elementConstraints = schemaToConstraints(schema.items)
        return RawArrayTypeDO(elementType = elementType, nullable = nullable, elementConstraints = elementConstraints)
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

private fun schemaToConstraints(schema: Schema<*>?): ConstraintsDO {
    if (schema == null) return ConstraintsDO()

    val stringConstraints: ConstraintsDO.StringConstraintsDO? =
        if (schema.type == "string" || schema.format != null || schema.pattern != null) {
            val minLength = schema.minLength
            val maxLength = schema.maxLength
            val pattern = schema.pattern
            if (minLength != null || maxLength != null || pattern != null) {
                ConstraintsDO.StringConstraintsDO(
                    minLength = minLength,
                    maxLength = maxLength,
                    pattern = pattern,
                )
            } else null
        } else null

    val numberConstraints: ConstraintsDO.NumberConstraintsDO? =
        if (schema.type == "integer" || schema.type == "number") {
            val min = schema.minimum?.let { v ->
                ConstraintsDO.BoundDO(
                    value = v.toBigDecimalCompat(),
                    inclusive = schema.exclusiveMinimum != true,
                )
            }
            val max = schema.maximum?.let { v ->
                ConstraintsDO.BoundDO(
                    value = v.toBigDecimalCompat(),
                    inclusive = schema.exclusiveMaximum != true,
                )
            }
            val multipleOf = schema.multipleOf?.toBigDecimalCompat()
            if (min != null || max != null || multipleOf != null) {
                ConstraintsDO.NumberConstraintsDO(
                    min = min,
                    max = max,
                    multipleOf = multipleOf,
                )
            } else null
        } else null

    val arrayConstraints: ConstraintsDO.ArrayConstraintsDO? =
        if (schema is ArraySchema || schema.type == "array") {
            val minItems = schema.minItems
            val maxItems = schema.maxItems
            val uniqueItems = schema.uniqueItems
            if (minItems != null || maxItems != null || uniqueItems != null) {
                ConstraintsDO.ArrayConstraintsDO(
                    minItems = minItems,
                    maxItems = maxItems,
                    uniqueItems = uniqueItems,
                )
            } else null
        } else null

    val objectConstraints: ConstraintsDO.ObjectConstraintsDO? =
        if (schema.type == "object" || schema.properties != null || schema.additionalProperties != null) {
            val additional = schema.additionalProperties
            val additionalAllowed: Boolean? =
                when (additional) {
                    is Boolean -> additional
                    is Schema<*> -> true
                    else -> null
                }

            val minProps = schema.minProperties
            val maxProps = schema.maxProperties
            if (minProps != null || maxProps != null || additionalAllowed != null) {
                ConstraintsDO.ObjectConstraintsDO(
                    minProperties = minProps,
                    maxProperties = maxProps,
                    additionalPropertiesAllowed = additionalAllowed,
                )
            } else null
        } else null

    return ConstraintsDO(
        string = stringConstraints,
        number = numberConstraints,
        array = arrayConstraints,
        obj = objectConstraints,
    )
}

private fun Number.toBigDecimalCompat(): BigDecimal =
    when (this) {
        is BigDecimal -> this
        is Long -> BigDecimal.valueOf(this)
        is Int -> BigDecimal.valueOf(this.toLong())
        is Double -> BigDecimal.valueOf(this)
        is Float -> BigDecimal.valueOf(this.toDouble())
        else -> BigDecimal(this.toString())
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
        constraints = mergeConstraints(a.constraints, b.constraints),
    )

private fun mergeConstraints(
    a: ConstraintsDO,
    b: ConstraintsDO,
): ConstraintsDO =
    ConstraintsDO(
        string = mergeStringConstraints(a.string, b.string),
        number = mergeNumberConstraints(a.number, b.number),
        array = mergeArrayConstraints(a.array, b.array),
        obj = mergeObjectConstraints(a.obj, b.obj),
    )

private fun mergeStringConstraints(
    a: ConstraintsDO.StringConstraintsDO?,
    b: ConstraintsDO.StringConstraintsDO?,
): ConstraintsDO.StringConstraintsDO? {
    if (a == null) return b
    if (b == null) return a

    val minLength = listOfNotNull(a.minLength, b.minLength).maxOrNull()
    val maxLength = listOfNotNull(a.maxLength, b.maxLength).minOrNull()

    val pattern = b.pattern ?: a.pattern

    return ConstraintsDO.StringConstraintsDO(
        minLength = minLength,
        maxLength = maxLength,
        pattern = pattern,
    )
}

private fun mergeNumberConstraints(
    a: ConstraintsDO.NumberConstraintsDO?,
    b: ConstraintsDO.NumberConstraintsDO?,
): ConstraintsDO.NumberConstraintsDO? {
    if (a == null) return b
    if (b == null) return a

    val min = mergeMinBound(a.min, b.min)
    val max = mergeMaxBound(a.max, b.max)

    val multipleOf = b.multipleOf ?: a.multipleOf

    return ConstraintsDO.NumberConstraintsDO(
        min = min,
        max = max,
        multipleOf = multipleOf,
    )
}

private fun mergeMinBound(
    a: ConstraintsDO.BoundDO?,
    b: ConstraintsDO.BoundDO?,
): ConstraintsDO.BoundDO? {
    if (a == null) return b
    if (b == null) return a

    val cmp = a.value.compareTo(b.value)
    return when {
        cmp > 0 -> a
        cmp < 0 -> b
        else -> {
            // Same numeric bound: exclusive is stricter (inclusive=false).
            if (a.inclusive == b.inclusive) a
            else if (!a.inclusive) a else b
        }
    }
}

private fun mergeMaxBound(
    a: ConstraintsDO.BoundDO?,
    b: ConstraintsDO.BoundDO?,
): ConstraintsDO.BoundDO? {
    if (a == null) return b
    if (b == null) return a

    val cmp = a.value.compareTo(b.value)
    return when {
        cmp < 0 -> a
        cmp > 0 -> b
        else -> {
            // Same numeric bound: exclusive is stricter (inclusive=false).
            if (a.inclusive == b.inclusive) a
            else if (!a.inclusive) a else b
        }
    }
}

private fun mergeArrayConstraints(
    a: ConstraintsDO.ArrayConstraintsDO?,
    b: ConstraintsDO.ArrayConstraintsDO?,
): ConstraintsDO.ArrayConstraintsDO? {
    if (a == null) return b
    if (b == null) return a

    val minItems = listOfNotNull(a.minItems, b.minItems).maxOrNull()
    val maxItems = listOfNotNull(a.maxItems, b.maxItems).minOrNull()

    val uniqueItems: Boolean? =
        when {
            a.uniqueItems == true || b.uniqueItems == true -> true
            a.uniqueItems == false && b.uniqueItems == false -> false
            else -> a.uniqueItems ?: b.uniqueItems
        }

    return ConstraintsDO.ArrayConstraintsDO(
        minItems = minItems,
        maxItems = maxItems,
        uniqueItems = uniqueItems,
    )
}

private fun mergeObjectConstraints(
    a: ConstraintsDO.ObjectConstraintsDO?,
    b: ConstraintsDO.ObjectConstraintsDO?,
): ConstraintsDO.ObjectConstraintsDO? {
    if (a == null) return b
    if (b == null) return a

    val minProperties = listOfNotNull(a.minProperties, b.minProperties).maxOrNull()
    val maxProperties = listOfNotNull(a.maxProperties, b.maxProperties).minOrNull()

    val additionalAllowed: Boolean? =
        when {
            a.additionalPropertiesAllowed == false || b.additionalPropertiesAllowed == false -> false
            a.additionalPropertiesAllowed == true || b.additionalPropertiesAllowed == true -> true
            else -> null
        }

    return ConstraintsDO.ObjectConstraintsDO(
        minProperties = minProperties,
        maxProperties = maxProperties,
        additionalPropertiesAllowed = additionalAllowed,
    )
}
