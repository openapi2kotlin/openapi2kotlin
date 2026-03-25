package dev.openapi2kotlin.adapter.parser.internal

import dev.openapi2kotlin.application.core.openapi2kotlin.model.raw.RawSchemaDO.ConstraintsDO
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.Schema
import java.math.BigDecimal

internal fun schemaToConstraints(schema: Schema<*>?): ConstraintsDO {
    if (schema == null) return ConstraintsDO()

    return ConstraintsDO(
        string = stringConstraints(schema),
        number = numberConstraints(schema),
        array = arrayConstraints(schema),
        obj = objectConstraints(schema),
    )
}

private fun stringConstraints(schema: Schema<*>): ConstraintsDO.StringConstraintsDO? {
    val supportsStringConstraints = schema.type == "string" || schema.format != null || schema.pattern != null
    if (!supportsStringConstraints) return null

    val minLength = schema.minLength
    val maxLength = schema.maxLength
    val pattern = schema.pattern
    val hasConstraints = minLength != null || maxLength != null || pattern != null

    return if (hasConstraints) {
        ConstraintsDO.StringConstraintsDO(
            minLength = minLength,
            maxLength = maxLength,
            pattern = pattern,
        )
    } else {
        null
    }
}

private fun numberConstraints(schema: Schema<*>): ConstraintsDO.NumberConstraintsDO? {
    val supportsNumberConstraints = schema.type == "integer" || schema.type == "number"
    if (!supportsNumberConstraints) return null

    val min = schema.minimum?.toBound(schema.exclusiveMinimum != true)
    val max = schema.maximum?.toBound(schema.exclusiveMaximum != true)
    val multipleOf = schema.multipleOf?.toBigDecimalCompat()
    val hasConstraints = min != null || max != null || multipleOf != null

    return if (hasConstraints) {
        ConstraintsDO.NumberConstraintsDO(
            min = min,
            max = max,
            multipleOf = multipleOf,
        )
    } else {
        null
    }
}

private fun arrayConstraints(schema: Schema<*>): ConstraintsDO.ArrayConstraintsDO? {
    val supportsArrayConstraints = schema is ArraySchema || schema.type == "array"
    if (!supportsArrayConstraints) return null

    val minItems = schema.minItems
    val maxItems = schema.maxItems
    val uniqueItems = schema.uniqueItems
    val hasConstraints = minItems != null || maxItems != null || uniqueItems != null

    return if (hasConstraints) {
        ConstraintsDO.ArrayConstraintsDO(
            minItems = minItems,
            maxItems = maxItems,
            uniqueItems = uniqueItems,
        )
    } else {
        null
    }
}

private fun objectConstraints(schema: Schema<*>): ConstraintsDO.ObjectConstraintsDO? {
    val supportsObjectConstraints =
        schema.type == "object" || schema.properties != null || schema.additionalProperties != null
    if (!supportsObjectConstraints) return null

    val additionalAllowed =
        when (val additional = schema.additionalProperties) {
            is Boolean -> additional
            is Schema<*> -> true
            else -> null
        }
    val minProps = schema.minProperties
    val maxProps = schema.maxProperties
    val hasConstraints = minProps != null || maxProps != null || additionalAllowed != null

    return if (hasConstraints) {
        ConstraintsDO.ObjectConstraintsDO(
            minProperties = minProps,
            maxProperties = maxProps,
            additionalPropertiesAllowed = additionalAllowed,
        )
    } else {
        null
    }
}

private fun Number.toBound(inclusive: Boolean): ConstraintsDO.BoundDO =
    ConstraintsDO.BoundDO(
        value = toBigDecimalCompat(),
        inclusive = inclusive,
    )

private fun Number.toBigDecimalCompat(): BigDecimal =
    when (this) {
        is BigDecimal -> this
        is Long -> BigDecimal.valueOf(this)
        is Int -> BigDecimal.valueOf(this.toLong())
        is Double -> BigDecimal.valueOf(this)
        is Float -> BigDecimal.valueOf(this.toDouble())
        else -> BigDecimal(this.toString())
    }
