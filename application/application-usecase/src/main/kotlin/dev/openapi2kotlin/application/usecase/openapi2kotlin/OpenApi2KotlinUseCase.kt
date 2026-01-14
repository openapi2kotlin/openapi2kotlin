package dev.openapi2kotlin.application.usecase.openapi2kotlin

import java.nio.file.Path

const val DEFAULT_PACKAGE_NAME = "dev.openapi2kotlin"

fun interface OpenApi2KotlinUseCase {
    fun openApi2kotlin(config: Config)

    data class Config(
        val inputSpecPath: Path,
        val outputDirPath: Path,

        val model: ModelConfig = ModelConfig(),

        /**
         * API generation target.
         *
         * - null        -> generate models only
         * - ApiConfig.Client -> generate client APIs
         * - ApiConfig.Server -> generate server APIs
         *
         * Exactly one API target may be specified per invocation.
         */
        val api: ApiConfig? = null,
    )

    data class ModelConfig(
        val packageName: String = "$DEFAULT_PACKAGE_NAME.model",

        val annotations: ModelAnnotationsConfig = ModelAnnotationsConfig(),
        val mapping: MappingConfig = MappingConfig(),
    ) {
        /**
         * Which annotations to emit into the generated model.
         */
        data class ModelAnnotationsConfig(
            /**
             * Jackson-related generation settings, fully encapsulated here.
             */
            val jackson: JacksonConfig = JacksonConfig(),

            /**
             * Validation annotations are useful primarily for server-side request validation.
             *
             * Many client-only use cases do not want to depend on jakarta.validation and
             * do not run validation automatically on responses.
             *
             * Precedence rules (Gradle plugin behavior):
             *  1) If the user explicitly sets model.annotations.validations.enabled -> that value wins.
             *  2) Else if API target is Server -> validations.enabled defaults to true.
             *  3) Else (client API or no API) -> validations.enabled defaults to false.
             */
            val validations: ValidationAnnotationsConfig = ValidationAnnotationsConfig(),
        ) {
            /**
             * Jackson-specific configuration: property-name mapping
             * and polymorphism / discriminator handling.
             */
            data class JacksonConfig(
                /**
                 * Master switch for emitting any Jackson annotations at all.
                 *
                 * - When false: no @JsonProperty, no @JsonTypeInfo, no @JsonSubTypes, etc.
                 * - When true: other switches below apply if enabled.
                 */
                val enabled: Boolean = true,

                /**
                 * Generate @JsonProperty for fields where originalName != generatedName
                 * (e.g. "@type" -> "atType", "foo-bar" -> "fooBar").
                 *
                 * This is independent of polymorphism; it is plain JSON name mapping.
                 */
                val jsonPropertyMapping: Boolean = true,

                /**
                 * Whether discriminator properties should be defaulted in constructors
                 * for ergonomics (e.g. override val atType: String = "Category_FVO").
                 */
                val defaultDiscriminatorValue: Boolean = true,

                /**
                 * Strict serialization prevents user-provided discriminator values
                 * from affecting JSON output. Jackson emits the discriminator based
                 * on the runtime subtype, not the field value.
                 */
                val strictDiscriminatorSerialization: Boolean = true,

                /**
                 * Generate @JsonValue on enum `value` properties.
                 */
                val jsonValue: Boolean = true,

                /**
                 * Generate @JsonCreator on enum fromValue() factory methods.
                 */
                val jsonCreator: Boolean = true,
            )

            data class ValidationAnnotationsConfig(
                /**
                 * Master switch for emitting validation annotations.
                 *
                 * When disabled, no @Valid, @Size, @Pattern, etc. are generated.
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
                            ValidationAnnotationsNamespace.entries.firstOrNull {
                                it.value.equals(value, ignoreCase = true)
                            } ?: throw IllegalArgumentException(
                                "Unexpected ValidationNamespace value: '$value'"
                            )
                    }
                }
            }
        }

        /**
         * Type-mapping knobs for the generated model.
         */
        data class MappingConfig(
            val double2BigDecimal: Boolean = true,
            val float2BigDecimal: Boolean = true,
            val integer2Long: Boolean = true,
        )
    }

    /**
     * API generation target.
     *
     * This sealed hierarchy intentionally makes invalid configurations
     * (e.g. client and server at the same time) unrepresentable.
     */
    sealed interface ApiConfig {
        /**
         * Base package for generated API sources.
         */
        val packageName: String

        /**
         * Client-side API generation.
         *
         * Intended for SDKs and consumers of remote APIs.
         */
        data class Client(
            override val packageName: String = "$DEFAULT_PACKAGE_NAME.client",
        ) : ApiConfig

        /**
         * Server-side API generation.
         *
         * Intended for API providers exposing HTTP endpoints.
         */
        data class Server(
            override val packageName: String = "$DEFAULT_PACKAGE_NAME.server",
            val swagger: SwaggerConfig = SwaggerConfig(),
            val framework: Framework = Framework.KTOR,
        ) : ApiConfig {
            /**
             * Supported server frameworks.
             */
            enum class Framework(
                val value: String,
            ) {
                KTOR("ktor"),
                SPRING("spring");

                override fun toString(): String = value

                companion object {
                    fun fromValue(value: String): Framework =
                        entries.firstOrNull { it.value.equals(value, ignoreCase = true) }
                            ?: throw IllegalArgumentException(
                                "Unexpected Framework value: '$value'"
                            )
                }
            }

            data class SwaggerConfig(
                val enabled: Boolean = false,
            )
        }
    }
}
