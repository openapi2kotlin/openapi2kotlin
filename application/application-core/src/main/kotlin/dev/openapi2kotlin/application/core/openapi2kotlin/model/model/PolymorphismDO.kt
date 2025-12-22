package dev.openapi2kotlin.application.core.openapi2kotlin.model.model

data class PolymorphismDO (
    /**
     * Discriminator property name from OpenAPI, e.g. "@type".
     */
    val discriminatorPropertyOriginalName: String,

    /**
     * Kotlin property name for discriminator, e.g. "atType".
     */
    val discriminatorPropertyGeneratedName: String,

    /**
     * For each child schema, which discriminator value must be emitted/accepted.
     *
     * Direction:
     *  schema NAME -> discriminator VALUE
     *
     * Example:
     *  "Category_FVO"     -> "Category_FVO"
     *  "CategoryRef_FVO"  -> "CategoryRef_FVO"
     */
    val schemaNameToDiscriminatorValue: Map<String, String>,
)