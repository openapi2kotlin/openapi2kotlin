package dev.openapi2kotlin.application.core.openapi2kotlin.model.model

/**
 * Shape that will be generated for the schema.
 */
sealed interface ModelShapeDO {

    /**
     * Initial placeholder before we decide.
     */
    data object Undecided : ModelShapeDO

    data class SealedInterface(
        /**
         * Parent interfaces/classes (by schema name) this sealed interface extends/implements.
         */
        val extends: List<String>,
    ) : ModelShapeDO

    data class DataClass(
        /**
         * Single parent class (by schema name), if any.
         */
        val extend: String?,
        /**
         * Interfaces (by schema name) this class implements.
         */
        val implements: List<String>,
    ) : ModelShapeDO

    data class OpenClass(
        /**
         * Single parent class (by schema name), if any.
         */
        val extend: String?,
        /**
         * Interfaces (by schema name) this class implements.
         */
        val implements: List<String>,
    ) : ModelShapeDO

    data class EnumClass(
        /**
         * Enum values.
         */
        val values: List<String>,
    ) : ModelShapeDO

    /**
     * Type alias shape.
     *
     * The final alias target will be mapped to KotlinPoet TypeName in the writer.
     */
    data class TypeAlias(
        val target: FieldTypeDO,
    ) : ModelShapeDO
}