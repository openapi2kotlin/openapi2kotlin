package dev.openapi2kotlin.adapter.parser.internal

import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.ListTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.PrimitiveTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.RefTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.raw.RawSchemaDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.FieldTypeDO
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.Schema

internal fun OpenAPI.toRawSchemas(): List<RawSchemaDO> {
    val schemas: Map<String, Schema<*>> = this.components?.schemas.orEmpty()

    // --- detect "usedAsProperty" ---
    val usedAsPropertyNames = mutableSetOf<String>()
    schemas.values.forEach { ownerSchema ->
        ownerSchema.properties?.values?.forEach { propertySchema ->
            collectRefNamesFromSchema(propertySchema, usedAsPropertyNames)
        }
    }

    // --- detect "usedInPaths" ---
    val usedInPathsNames = mutableSetOf<String>()
    this.paths?.values?.forEach { pathItem ->
        pathItem.readOperations().forEach { operation ->
            // request body
            operation.requestBody?.content?.values?.forEach { mediaType ->
                collectRefNamesFromSchema(mediaType.schema, usedInPathsNames)
            }
            // responses
            operation.responses?.values?.forEach { apiResponse ->
                apiResponse.content?.values?.forEach { mediaType ->
                    collectRefNamesFromSchema(mediaType.schema, usedInPathsNames)
                }
            }
            // parameters
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
        val arrayItemType: FieldTypeDO? =
            if (isArraySchema && schema is ArraySchema) {
                // element type for the "typealias X = List<Elem>"
                schemaToDtoTypeForProperty(
                    schema = schema.items,
                    required = true, // element itself non-null; list nullability handled later
                )
            } else null

        // ---- own properties: top-level + inline allOf objects ----
        val ownProps = mutableMapOf<String, RawSchemaDO.SchemaPropertyDO>()

        // top-level properties
        val requiredTop = schema.required?.toSet().orEmpty()
        schema.properties.orEmpty().forEach { (propName, propSchema) ->
            val required = propName in requiredTop
            val dtoType = schemaToDtoTypeForProperty(propSchema, required)
            ownProps.merge(
                propName,
                RawSchemaDO.SchemaPropertyDO(propName, dtoType, required),
                ::mergeSchemaProperty,
            )
        }

        // inline allOf parts (type: object, no $ref)
        schema.allOf
            ?.filter { it.`$ref` == null }
            ?.forEach { inlineSchema ->
                val requiredInline = inlineSchema.required?.toSet().orEmpty()
                inlineSchema.properties.orEmpty().forEach { (propName, propSchema) ->
                    val required = propName in requiredInline
                    val dtoType = schemaToDtoTypeForProperty(propSchema, required)
                    ownProps.merge(
                        propName,
                        RawSchemaDO.SchemaPropertyDO(propName, dtoType, required),
                        ::mergeSchemaProperty,
                    )
                }
            }

        val usedInPaths = name in usedInPathsNames
        val usedAsProperty = name in usedAsPropertyNames

        RawSchemaDO(
            originalName = name,
            allOfParents = allOfParents,
            oneOfChildren = oneOfChildren,
            enumValues = enumValues,
            isArraySchema = isArraySchema,
            arrayItemType = arrayItemType,
            ownProperties = ownProps,
            discriminatorPropertyName = schema.discriminator?.propertyName,
            usedInPaths = usedInPaths,
            usedAsProperty = usedAsProperty,
        )
    }.sortedBy { it.originalName }
}

/* ---------- helpers (still OpenAPI-dependent, but local to this file) ---------- */

private fun schemaToDtoTypeForProperty(
    schema: Schema<*>?,
    required: Boolean,
): FieldTypeDO {
    if (schema == null) {
        return PrimitiveTypeDO(PrimitiveTypeDO.PrimitiveTypeNameDO.ANY, nullable = true)
    }

    val nullableFromRequired = !required

    if (schema is ArraySchema) {
        val elementType = schemaToDtoTypeForProperty(
            schema = schema.items,
            required = true, // element nullability is separate concern
        )
        val nullable = schema.nullable == true || nullableFromRequired
        return ListTypeDO(
            elementType = elementType,
            nullable = nullable,
        )
    }

    schema.`$ref`?.let { ref ->
        val name = ref.substringAfterLast('/')
        val nullable = schema.nullable == true || nullableFromRequired
        return RefTypeDO(
            schemaName = name,
            nullable = nullable,
        )
    }

    val kind: PrimitiveTypeDO.PrimitiveTypeNameDO = when (schema.type) {
        "string" -> PrimitiveTypeDO.PrimitiveTypeNameDO.STRING
        "integer" -> if (schema.format == "int64") PrimitiveTypeDO.PrimitiveTypeNameDO.LONG else PrimitiveTypeDO.PrimitiveTypeNameDO.INT
        "number" -> PrimitiveTypeDO.PrimitiveTypeNameDO.DOUBLE
        "boolean" -> PrimitiveTypeDO.PrimitiveTypeNameDO.BOOLEAN
        else -> PrimitiveTypeDO.PrimitiveTypeNameDO.ANY
    }

    val nullable = schema.nullable == true || nullableFromRequired
    return PrimitiveTypeDO(kind, nullable)
}

private fun collectRefNamesFromSchema(
    schema: Schema<*>?,
    into: MutableSet<String>,
) {
    if (schema == null) return

    when (schema) {
        is ArraySchema -> collectRefNamesFromSchema(schema.items, into)
        else -> {
            val ref = schema.`$ref` ?: return
            val name = ref.substringAfterLast('/')
            into.add(name)
        }
    }
}

private fun mergeSchemaProperty(a: RawSchemaDO.SchemaPropertyDO, b: RawSchemaDO.SchemaPropertyDO): RawSchemaDO.SchemaPropertyDO =
    RawSchemaDO.SchemaPropertyDO(
        name = a.name,
        type = a.type, // type should be equivalent; if not, first wins
        required = a.required || b.required,
    )
