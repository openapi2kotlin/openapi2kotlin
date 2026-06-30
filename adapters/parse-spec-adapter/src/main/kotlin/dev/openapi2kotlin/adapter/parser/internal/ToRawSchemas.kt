package dev.openapi2kotlin.adapter.parser.internal

import dev.openapi2kotlin.application.core.openapi2kotlin.domain.raw.RawSchemaDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.raw.RawSchemaDO.RawArrayTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.raw.RawSchemaDO.RawFieldTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.raw.RawSchemaDO.RawMapTypeDO
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
    val inlineEnums = schemas.collectInlineEnumSchemas()

    return (
        schemas.entries.map { entry ->
            entry.toRawSchema(
                usedAsPropertyNames = usedAsPropertyNames,
                usedInPathsNames = usedInPathsNames,
                inlineEnumNames =
                    inlineEnums
                        .filter { it.ownerSchemaName == entry.key }
                        .associate { it.propertyName to it.schemaName },
            )
        } + inlineEnums.map { it.toRawSchema() }
    ).sortedBy { it.originalName }
}

internal fun schemaToRawTypeForProperty(
    schema: Schema<*>?,
    required: Boolean,
): RawFieldTypeDO {
    if (schema == null) return RawPrimitiveTypeDO(RawPrimitiveTypeDO.Type.OBJECT, format = null, nullable = true)

    val effectiveType = schema.effectiveType()
    val nullable = schema.isNullable(required)
    val refName = schema.`$ref`?.substringAfterLast('/') ?: schema.singleComposedRefName()

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

        schema.additionalProperties is Schema<*> ->
            RawMapTypeDO(
                valueType = schemaToRawTypeForProperty(schema.additionalProperties as Schema<*>, required = true),
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

private data class InlineEnumSchema(
    val ownerSchemaName: String,
    val propertyName: String,
    val schemaName: String,
    val values: List<String>,
    val description: String?,
)

private fun Map<String, Schema<*>>.collectInlineEnumSchemas(): List<InlineEnumSchema> {
    val existingSchemaNames = keys

    return flatMap { (ownerName, ownerSchema) ->
        ownerSchema.properties.orEmpty().mapNotNull { (propertyName, propertySchema) ->
            val values = propertySchema.enum?.map { it.toString() }.orEmpty()
            val discriminatorPropertyName = ownerSchema.discriminator?.propertyName
            if (values.isEmpty() || propertyName == discriminatorPropertyName) return@mapNotNull null

            val preferredName = ownerName.inlineEnumOwnerName() + propertyName.toPascalCase()
            val schemaName =
                preferredName.takeUnless { it in existingSchemaNames }
                    ?: ownerName + propertyName.toPascalCase()

            InlineEnumSchema(
                ownerSchemaName = ownerName,
                propertyName = propertyName,
                schemaName = schemaName,
                values = values,
                description = propertySchema.description,
            )
        }
    }
}

private fun InlineEnumSchema.toRawSchema(): RawSchemaDO =
    RawSchemaDO(
        originalName = schemaName,
        allOfParents = emptyList(),
        oneOfChildren = emptyList(),
        enumValues = values,
        constraints = RawSchemaDO.ConstraintsDO(),
        ownProperties = emptyMap(),
        discriminatorPropertyName = null,
        discriminatorMapping = emptyMap(),
        isDiscriminatorSelfMapped = false,
        usedInPaths = false,
        usedAsProperty = true,
        description = description,
    )

private fun String.inlineEnumOwnerName(): String =
    removeSuffix("Widget")
        .removeSuffix("Dto")

private fun String.toPascalCase(): String =
    split(Regex("[^A-Za-z0-9]+"))
        .filter { it.isNotBlank() }
        .joinToString("") { token ->
            token.replaceFirstChar { char ->
                if (char.isLowerCase()) char.titlecase() else char.toString()
            }
        }

private fun Schema<*>.singleComposedRefName(): String? =
    singleComposedRef()
        ?.`$ref`
        ?.substringAfterLast('/')

private fun Schema<*>.singleComposedRef(): Schema<*>? {
    val children = allOf ?: anyOf ?: oneOf ?: return null
    return children.singleOrNull { it.`$ref` != null }
        ?.takeIf { children.size == 1 }
}
