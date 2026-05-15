package dev.openapi2kotlin.adapter.parser.internal

import dev.openapi2kotlin.application.core.openapi2kotlin.domain.raw.RawSchemaDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.raw.RawSchemaDO.RawArrayTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.raw.RawSchemaDO.RawFieldTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.raw.RawSchemaDO.RawPrimitiveTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.raw.RawSchemaDO.RawRefTypeDO
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.ComposedSchema
import io.swagger.v3.oas.models.media.Schema

internal fun OpenAPI.toRawSchemas(): List<RawSchemaDO> {
    val schemas: Map<String, Schema<*>> = components?.schemas.orEmpty()
    val usedAsPropertyNames = schemas.collectUsedAsPropertyNames()
    val usedInPathsNames = collectUsedInPathsNames()

    return schemas.entries
        .map { entry ->
            entry.toRawSchema(
                usedAsPropertyNames = usedAsPropertyNames,
                usedInPathsNames = usedInPathsNames,
            )
        }.sortedBy { it.originalName }
}

internal fun schemaToRawTypeForProperty(
    schema: Schema<*>?,
    required: Boolean,
): RawFieldTypeDO {
    if (schema == null) return RawPrimitiveTypeDO(RawPrimitiveTypeDO.Type.OBJECT, format = null, nullable = true)

    val effectiveType = schema.effectiveType()
    val nullable = schema.isNullable(required)
    val refName = schema.`$ref`?.substringAfterLast('/')

    return when {
        schema.isArraySchemaLike(effectiveType) ->
            RawArrayTypeDO(
                elementType = schemaToRawTypeForProperty(schema.items, required = true),
                nullable = nullable,
                elementConstraints = schemaToConstraints(schema.items),
            )

        refName != null ->
            RawRefTypeDO(
                schemaName = refName,
                nullable = nullable,
            )

        else ->
            RawPrimitiveTypeDO(
                type = effectiveType.toRawPrimitiveType(),
                format = schema.format,
                nullable = nullable,
            )
    }
}

internal fun collectRefNamesFromSchema(
    schema: Schema<*>?,
    into: MutableSet<String>,
) {
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

internal fun mergeSchemaProperty(
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

private fun String?.toRawPrimitiveType(): RawPrimitiveTypeDO.Type =
    when (this) {
        "string" -> RawPrimitiveTypeDO.Type.STRING
        "number" -> RawPrimitiveTypeDO.Type.NUMBER
        "integer" -> RawPrimitiveTypeDO.Type.INTEGER
        "boolean" -> RawPrimitiveTypeDO.Type.BOOLEAN
        "object" -> RawPrimitiveTypeDO.Type.OBJECT
        else -> RawPrimitiveTypeDO.Type.OBJECT
    }

internal fun Schema<*>.effectiveType(): String? {
    val parsedTypes = types.orEmpty().filter { it.isNotBlank() }
    val nonNullTypes = parsedTypes.filterNot { it == "null" }

    return when {
        type != null -> type
        nonNullTypes.size == 1 -> nonNullTypes.single()
        "array" in nonNullTypes -> "array"
        "object" in nonNullTypes -> "object"
        else -> nonNullTypes.firstOrNull()
    }
}

internal fun Schema<*>.isNullable(required: Boolean): Boolean =
    nullable == true || types.orEmpty().contains("null") || !required

internal fun Schema<*>.isArraySchemaLike(effectiveType: String? = effectiveType()): Boolean =
    this is ArraySchema || effectiveType == "array"
