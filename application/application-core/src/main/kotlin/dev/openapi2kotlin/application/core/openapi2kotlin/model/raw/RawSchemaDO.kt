package dev.openapi2kotlin.application.core.openapi2kotlin.model.raw

import java.math.BigDecimal

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
     * Schema-level validation constraints, if any (e.g., typealias schemas such as "X: array" with minItems/maxItems).
     */
    val constraints: ConstraintsDO = ConstraintsDO(),

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
    /**
     * Raw constraint snapshot derived from OpenAPI Schema keywords.
     */
    data class ConstraintsDO(
        val string: StringConstraintsDO? = null,
        val number: NumberConstraintsDO? = null,
        val array: ArrayConstraintsDO? = null,
        val obj: ObjectConstraintsDO? = null,
    ) {
        data class StringConstraintsDO(
            val minLength: Int? = null,
            val maxLength: Int? = null,
            val pattern: String? = null,
        )

        data class BoundDO(
            val value: BigDecimal,
            val inclusive: Boolean,
        )

        data class NumberConstraintsDO(
            val min: BoundDO? = null,
            val max: BoundDO? = null,
            val multipleOf: BigDecimal? = null,
        )

        data class ArrayConstraintsDO(
            val minItems: Int? = null,
            val maxItems: Int? = null,
            val uniqueItems: Boolean? = null,
        )

        data class ObjectConstraintsDO(
            val minProperties: Int? = null,
            val maxProperties: Int? = null,

            /**
             * OpenAPI "additionalProperties" flag when it is a boolean. If the value is a Schema, this is considered allowed.
             * Null means unspecified by the source schema.
             */
            val additionalPropertiesAllowed: Boolean? = null,
        )
    }

    data class SchemaPropertyDO(
        val name: String,
        val type: RawFieldTypeDO,
        val required: Boolean,
        val defaultValue: String? = null,
        val description: String? = null,

        /**
         * Property-level validation constraints derived from the property schema.
         */
        val constraints: ConstraintsDO = ConstraintsDO(),
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

        /**
         * Constraints that apply to the array items schema itself (not the list size).
         * Used for container element constraints / nested validators.
         */
        val elementConstraints: ConstraintsDO = ConstraintsDO(),
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