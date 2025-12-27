package dev.openapi2kotlin.application.core.openapi2kotlin.model.model

/**
 * Field of a schema.
 *
 * @property originalName  name in OpenAPI (can be "@type", "foo-bar", ...)
 * @property generatedName Kotlin identifier used in generated code ("atType", "fooBar", ...)
 */
data class FieldDO(
    val originalName: String,
    val generatedName: String,
    val overridden: Boolean,
    val type: FieldTypeDO,
    val required: Boolean,
    val defaultValueCode: String? = null,
    val annotations: List<ModelAnnotationDO> = emptyList(),
    val kdoc: String? = null,
)