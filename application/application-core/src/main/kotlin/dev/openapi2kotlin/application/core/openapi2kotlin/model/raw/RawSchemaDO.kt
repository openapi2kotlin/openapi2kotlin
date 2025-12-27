package dev.openapi2kotlin.application.core.openapi2kotlin.model.raw

/**
 * Raw schema data object representing an OpenAPI schema component. No business logic here - open api AS-IS ;)
 */
data class RawSchemaDO(
    /**
     * Name of the schema component (as in OpenAPI).
     */
    val originalName: String,

    /**
     * Schema description from OpenAPI (schema.description).
     */
    val description: String? = null,

    /**
     * Names of parents via allOf. This schema extends these via allOf.
     */
    val allOfParents: List<String>,

    /**
     * Names of children via oneOf. This schema's oneOf[...] entries.
     */
    val oneOfChildren: List<String>,

    /**
     * Enum raw values, if this schema is an enum string/integer.
     * Empty for all non-enum schemas.
     */
    val enumValues: List<String> = emptyList(),

    /**
     * Whether this schema is an array schema (items defined).
     */
    val isArraySchema: Boolean = false,

    /**
     * If this is an array schema, the type of its items.
     * (For typealias generation, we later wrap this into List<...>.)
     */
    val arrayItemType: RawFieldTypeDO? = null,

    /**
     * Properties defined directly on this schema (top-level + inline allOf).
     * Parent properties are not included here; they are resolved later.
     */
    val ownProperties: Map<String, SchemaPropertyDO> = emptyMap(),

    /**
     * Discriminator property name, if any (e.g. "@type").
     */
    val discriminatorPropertyName: String? = null,

    /**
     * Map: subtype Raw schema originalName -> wire discriminator id.
     */
    val discriminatorValueToSchemaName: Map<String, String> = emptyMap(),

    /**
     * Whether this schema is used in paths as a request or response.
     */
    val usedInPaths: Boolean = false,

    /**
     * Whether this schema is used as a property directly in another schema component.
     */
    val usedAsProperty: Boolean = false,

    /**
     * Discriminator mapping of given schema.
     */
    val discriminatorMapping: Map<String, String>,

    /**
     * Whether the discriminator maps to the same schema name as the discriminator value.
     */
    val isDiscriminatorSelfMapped: Boolean,
) {
    data class SchemaPropertyDO(
        val name: String,
        val type: RawFieldTypeDO,
        val required: Boolean,
        val defaultValue: String? = null,
        val description: String? = null,
    )

    sealed interface RawFieldTypeDO {
        val nullable: Boolean
    }

    data class RawRefTypeDO(
        val schemaName: String,
        override val nullable: Boolean,
    ) : RawFieldTypeDO

    data class RawArrayTypeDO(
        val elementType: RawFieldTypeDO,
        override val nullable: Boolean,
    ) : RawFieldTypeDO

    data class RawPrimitiveTypeDO(
        val type: Type,
        val format: String? = null,
        override val nullable: Boolean,
    ) : RawFieldTypeDO {
        enum class Type {
            STRING,
            NUMBER,
            INTEGER,
            BOOLEAN,
            OBJECT,
        }
    }
}