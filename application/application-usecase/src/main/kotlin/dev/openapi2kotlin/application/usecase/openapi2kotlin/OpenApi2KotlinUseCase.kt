package dev.openapi2kotlin.application.usecase.openapi2kotlin

import java.nio.file.Path

const val DEFAULT_PACKAGE_NAME = "dev.openapi2kotlin"

fun interface OpenApi2KotlinUseCase {
    fun openApi2kotlin(config: Config)

    data class Config(
        val inputSpecPath: Path,
        val outputDirPath: Path,

        val model: ModelConfig = ModelConfig(),
        val client: ClientConfig = ClientConfig(),
        val server: ServerConfig = ServerConfig(),
    )

    data class ModelConfig(
        val packageName: String = "$DEFAULT_PACKAGE_NAME.model",

        val annotations: AnnotationsConfig = AnnotationsConfig(),
        val mapping: MappingConfig = MappingConfig(),
    ) {
        /**
         * Which annotations to emit into the generated model.
         */
        data class AnnotationsConfig(
            /**
             * Jackson-related generation settings, fully encapsulated here.
             */
            val jackson: JacksonConfig = JacksonConfig(),
        )

        /**
         * Type mapping knobs for the generated model.
         */
        data class MappingConfig(
            val double2BigDecimal: Boolean = true,
            val float2BigDecimal: Boolean = true,
            val integer2Long: Boolean = true,
        )

        /**
         * Jackson-specific configuration: property-name mapping + polymorphism/discriminators.
         */
        data class JacksonConfig(
            /**
             * Master switch for emitting any Jackson annotations at all.
             *
             * - When false: no @JsonProperty, no @JsonTypeInfo, no @JsonSubTypes, etc.
             * - When true: other switches below apply if any.
             */
            val enabled: Boolean = true,

            /**
             * Generate @JsonProperty for fields where originalName != generatedName
             * (e.g. "@type" -> "atType", "foo-bar" -> "fooBar").
             *
             * This is independent of polymorphism: it is plain JSON name mapping.
             */
            val jsonPropertyMapping: Boolean = true,

            /**
             * Whether discriminator properties should be defaulted in constructors for ergonomics.
             * Example: override val atType: String = "Category_FVO"
             */
            val defaultDiscriminatorValue: Boolean = true,

            /**
             * Strict serialization prevents user-provided discriminator property values from
             * affecting JSON output. Jackson emits discriminator from runtime subtype, not the field.
             *
             * Typical implementation: @JsonIgnoreProperties(value=[disc], allowSetters=true) on union root.
             */
            val strictDiscriminatorSerialization: Boolean = true,
        )
    }

    data class ClientConfig(
        val enabled: Boolean = false,
        val packageName: String = "$DEFAULT_PACKAGE_NAME.client",
    )

    data class ServerConfig(
        val enabled: Boolean = false,
        val packageName: String = "$DEFAULT_PACKAGE_NAME.server",
        val framework: Framework = Framework.KTOR,
    ) {
        enum class Framework(
            val value: String,
        ) {
            KTOR("ktor"),
            SPRING("spring");

            override fun toString(): String = value

            companion object {
                fun fromValue(value: String): Framework =
                    Framework.entries.firstOrNull { it.value.equals(value, ignoreCase = true) }
                        ?: throw kotlin.IllegalArgumentException("Unexpected Framework value: '$value'")
            }
        }
    }
}
