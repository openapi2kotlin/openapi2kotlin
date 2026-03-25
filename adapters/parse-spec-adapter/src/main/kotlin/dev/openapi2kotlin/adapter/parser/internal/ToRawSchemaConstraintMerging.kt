package dev.openapi2kotlin.adapter.parser.internal

import dev.openapi2kotlin.application.core.openapi2kotlin.model.raw.RawSchemaDO.ConstraintsDO

internal fun mergeConstraints(
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
    val left = a
    val right = b
    return when {
        left == null -> right
        right == null -> left
        else ->
            ConstraintsDO.StringConstraintsDO(
                minLength = listOfNotNull(left.minLength, right.minLength).maxOrNull(),
                maxLength = listOfNotNull(left.maxLength, right.maxLength).minOrNull(),
                pattern = right.pattern ?: left.pattern,
            )
    }
}

private fun mergeNumberConstraints(
    a: ConstraintsDO.NumberConstraintsDO?,
    b: ConstraintsDO.NumberConstraintsDO?,
): ConstraintsDO.NumberConstraintsDO? {
    val left = a
    val right = b
    return when {
        left == null -> right
        right == null -> left
        else ->
            ConstraintsDO.NumberConstraintsDO(
                min = mergeMinBound(left.min, right.min),
                max = mergeMaxBound(left.max, right.max),
                multipleOf = right.multipleOf ?: left.multipleOf,
            )
    }
}

private fun mergeMinBound(
    a: ConstraintsDO.BoundDO?,
    b: ConstraintsDO.BoundDO?,
): ConstraintsDO.BoundDO? {
    val left = a
    val right = b
    return when {
        left == null -> right
        right == null -> left
        else -> {
            val cmp = left.value.compareTo(right.value)
            when {
                cmp > 0 -> left
                cmp < 0 -> right
                left.inclusive == right.inclusive -> left
                !left.inclusive -> left
                else -> right
            }
        }
    }
}

private fun mergeMaxBound(
    a: ConstraintsDO.BoundDO?,
    b: ConstraintsDO.BoundDO?,
): ConstraintsDO.BoundDO? {
    val left = a
    val right = b
    return when {
        left == null -> right
        right == null -> left
        else -> {
            val cmp = left.value.compareTo(right.value)
            when {
                cmp < 0 -> left
                cmp > 0 -> right
                left.inclusive == right.inclusive -> left
                !left.inclusive -> left
                else -> right
            }
        }
    }
}

private fun mergeArrayConstraints(
    a: ConstraintsDO.ArrayConstraintsDO?,
    b: ConstraintsDO.ArrayConstraintsDO?,
): ConstraintsDO.ArrayConstraintsDO? {
    val left = a
    val right = b
    return when {
        left == null -> right
        right == null -> left
        else ->
            ConstraintsDO.ArrayConstraintsDO(
                minItems = listOfNotNull(left.minItems, right.minItems).maxOrNull(),
                maxItems = listOfNotNull(left.maxItems, right.maxItems).minOrNull(),
                uniqueItems =
                    when {
                        left.uniqueItems == true || right.uniqueItems == true -> true
                        left.uniqueItems == false && right.uniqueItems == false -> false
                        else -> left.uniqueItems ?: right.uniqueItems
                    },
            )
    }
}

private fun mergeObjectConstraints(
    a: ConstraintsDO.ObjectConstraintsDO?,
    b: ConstraintsDO.ObjectConstraintsDO?,
): ConstraintsDO.ObjectConstraintsDO? {
    val left = a
    val right = b
    return when {
        left == null -> right
        right == null -> left
        else ->
            ConstraintsDO.ObjectConstraintsDO(
                minProperties = listOfNotNull(left.minProperties, right.minProperties).maxOrNull(),
                maxProperties = listOfNotNull(left.maxProperties, right.maxProperties).minOrNull(),
                additionalPropertiesAllowed =
                    when {
                        left.additionalPropertiesAllowed == false || right.additionalPropertiesAllowed == false -> false
                        left.additionalPropertiesAllowed == true || right.additionalPropertiesAllowed == true -> true
                        else -> null
                    },
            )
    }
}
