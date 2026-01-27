package dev.openapi2kotlin.application.core.openapi2kotlin.model.model

import dev.openapi2kotlin.application.core.openapi2kotlin.model.raw.RawSchemaDO

data class ModelDO(
    val rawSchema: RawSchemaDO,

    /**
     * Package where this schema resides.
     */
    val packageName: String,

    /**
     * Generated name of the schema component (cleaned).
     */
    val generatedName: String,

    /**
     * Who extends this via allOf (by generatedName).
     */
    val allOfChildren: MutableList<String> = mutableListOf(),

    /**
     * Who includes this in their oneOf (by generatedName).
     */
    val parentOneOf: MutableSet<String> = linkedSetOf(),

    /**
     * True if this schema is referenced by any discriminator mapping (i.e., it is a polymorphic subtype),
     * even if it is not referenced in paths or as a property.
     */
    var usedAsDiscriminatorChild: Boolean = false,

    /**
     * Generated shape of this schema component.
     */
    var modelShape: ModelShapeDO = ModelShapeDO.Undecided,

    /**
     * Final fields of this schema component (after inheritance, overrides, requiredness).
     */
    var fields: MutableList<FieldDO> = mutableListOf(),

    /**
     * Annotations to add to the generated class/interface.
     */
    var annotations: List<ModelAnnotationDO> = emptyList(),

    /**
     * Polymorphism metadata for oneOf roots (null if not polymorphic).
     */
    var polymorphism: PolymorphismDO? = null,

    /**
     * KDoc/description to render on the generated type (class/interface).
     * Prepared in core; generator must only print it.
     */
    var kdoc: String? = null,

    /**
     * Annotations to add to enum's `value` property.
     */
    var enumValueAnnotations: List<ModelAnnotationDO> = emptyList(),

    /**
     * Annotations to add to enum's `fromValue()` factory method.
     */
    var enumFromValueAnnotations: List<ModelAnnotationDO> = emptyList(),
)