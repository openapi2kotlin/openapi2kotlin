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

            /**
             * Validation annotations are useful primarily for server-side request validation. Many client-only
             * use cases do not want to depend on jakarta.validation and do not run validation automatically
             * on responses. Therefore, the Gradle plugin applies a conditional default:
             *
             * Precedence rules (Gradle plugin behavior):
             *  1) If the user explicitly sets model.annotations.validations.enabled -> that value wins.
             *  2) Else if server.enabled == true -> validations.enabled defaults to true.
             *  3) Else (no server) -> validations.enabled defaults to false.
             */
            val validations: ValidationAnnotationsConfig = ValidationAnnotationsConfig(),

            /**
             * Swagger annotation are useful primarily for server-side. Therefore, the plugin applies a conditional default:
             *
             * Precedence rules (Gradle plugin behavior):
             *  1) If the user explicitly sets model.annotations.swagger.schema -> that value wins.
             *  2) Else if server.enabled == true -> validations.enabled defaults to true.
             *  3) Else (no server) -> validations.enabled defaults to false.
             */
            val swagger: SwaggerConfig = SwaggerConfig(),
        ) {
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

                /**
                 * Generate @JsonValue on `value` property for enums.
                 */
                val jsonValue: Boolean = true,

                /**
                 * Generate @JsonCreator on fromValue() factory method for enums.
                 */
                val jsonCreator: Boolean = true,
            )

            data class ValidationAnnotationsConfig(
                /**
                 * Master switch for emitting any validation annotations at all.
                 *
                 * Note: the Gradle plugin may enable this by default when server.enabled=true unless the user
                 * explicitly overrides this flag.
                 *
                 * - When false: no @Valid, no @Size(min = 1), no @Pattern, etc.
                 * - When true: other switches below apply if any.
                 */
                val enabled: Boolean = true,

                /**
                 * Namespace for validation annotations: "jakarta" or "javax".
                 */
                val namespace: ValidationAnnotationsNamespace = ValidationAnnotationsNamespace.JAKARTA,
            ) {
                enum class ValidationAnnotationsNamespace(
                    val value: String,
                ) {
                    JAKARTA("jakarta"),
                    JAVAX("javax");

                    override fun toString(): String = value

                    companion object {
                        fun fromValue(value: String): ValidationAnnotationsNamespace =
                            ValidationAnnotationsNamespace.entries.firstOrNull { it.value.equals(value, ignoreCase = true) }
                                ?: throw kotlin.IllegalArgumentException("Unexpected ValidationNamespace value: '$value'")
                    }
                }
            }

            data class SwaggerConfig(
                /**
                 * Master switch for emitting any swagger annotations at all.
                 *
                 * Note: the Gradle plugin may enable this by default when server.enabled=true unless the user
                 * explicitly overrides this flag.
                 *
                 * - When false: no @Valid, no @Size(min = 1), no @Pattern, etc.
                 * - When true: other switches below apply if any.
                 */
                val enabled: Boolean = false,
            )
        }

        /**
         * Type mapping knobs for the generated model.
         */
        data class MappingConfig(
            val double2BigDecimal: Boolean = true,
            val float2BigDecimal: Boolean = true,
            val integer2Long: Boolean = true,
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
