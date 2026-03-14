package dev.openapi2kotlin.gradleplugin

import dev.openapi2kotlin.application.usecase.openapi2kotlin.OpenApi2KotlinUseCase
import org.gradle.api.GradleException

/**
 * Gradle DSL entrypoint for configuring `openapi2kotlin`.
 *
 * Defaults live here so the plugin exposes a single place that defines both
 * the public configuration surface and the effective generation behavior.
 */
open class OpenApi2KotlinExtension {
    /**
     * description: Path to OpenAPI YAML or JSON specification, e.g. "$projectDir/src/main/resources/openapi.yaml".
     * required: true
     */
    var inputSpec: String? = null

    /**
     * description: Root directory for generated Kotlin sources, e.g. layout.buildDirectory.dir("generated/src/main/kotlin").get().asFile.path.
     * required: true
     */
    var outputDir: String? = null

    /**
     * description: Enables or disables code generation for the current Gradle run.
     * default: true
     * values: true, false
     */
    var enabled: Boolean = true

    /** Generated model configuration. */
    val model = ModelConfigExtension()

    /** Generated API client configuration. Mutually exclusive with `server {}`. */
    var client: ClientExtension? = null
        private set

    /** Generated API server configuration. Mutually exclusive with `client {}`. */
    var server: ServerExtension? = null
        private set

    fun model(block: ModelConfigExtension.() -> Unit) = model.block()

    fun client(block: ClientExtension.() -> Unit) {
        if (server != null) throwClientServerExclusivityError()
        client = (client ?: ClientExtension()).apply(block)
    }

    fun server(block: ServerExtension.() -> Unit) {
        if (client != null) throwClientServerExclusivityError()
        server = (server ?: ServerExtension()).apply(block)
    }

    internal fun toUseCaseModelConfig(apiConfig: OpenApi2KotlinUseCase.ApiConfig?): OpenApi2KotlinUseCase.ModelConfig {
        val effectiveSerialization = model.serialization?.toUseCaseSerialization()
            ?: when (apiConfig) {
                is OpenApi2KotlinUseCase.ApiConfig.ClientRestClient,
                is OpenApi2KotlinUseCase.ApiConfig.ServerSpring,
                -> OpenApi2KotlinUseCase.ModelConfig.Serialization.JACKSON

                else -> OpenApi2KotlinUseCase.ModelConfig.Serialization.KOTLINX
            }

        val effectiveValidation = model.validation?.toUseCaseValidation()

        return OpenApi2KotlinUseCase.ModelConfig(
            packageName = model.packageName,
            serialization = effectiveSerialization,
            validation = effectiveValidation,
            double2BigDecimal = model.double2BigDecimal,
            float2BigDecimal = model.float2BigDecimal,
            integer2Long = model.integer2Long,
        )
    }

    private fun throwClientServerExclusivityError(): Nothing {
        throw GradleException(
            "openapi2kotlin: client{} and server{} cannot coexist.\n" +
                "This generator is intentionally single-target.\n" +
                "\n" +
                "If you feel the need to generate both in one run,\n" +
                "the issue is likely architectural rather than configurational.\n" +
                "\n" +
                "Choose exactly one:\n\n" +
                "openapi2kotlin {\n" +
                "    client { ... }\n" +
                "}\n" +
                "\n" +
                "or:\n\n" +
                "openapi2kotlin {\n" +
                "    server { ... }\n" +
                "}"
        )
    }

    /** Model generation options. */
    open class ModelConfigExtension {
        /**
         * description: Package name for generated model classes.
         * default: "dev.openapi2kotlin.model"
         */
        var packageName: String = DEFAULT_MODEL_PACKAGE_NAME

        /**
         * description: Serialization annotation family for generated model classes.
         * default: Ktor -> KotlinX, Server Spring -> Jackson, Client RestClient -> Jackson
         * values: KotlinX, Jackson
         */
        var serialization: Serialization? = null

        /**
         * description: Validation annotations namespace used in generated models.
         * default: None
         * values: None, Jakarta, JavaX
         */
        var validation: Validation? = null

        /**
         * description: Maps OpenAPI number/double to BigDecimal instead of Double.
         * default: false
         * values: true, false
         */
        var double2BigDecimal: Boolean = DEFAULT_DOUBLE_2_BIG_DECIMAL

        /**
         * description: Maps OpenAPI number/float to BigDecimal instead of Float.
         * default: false
         * values: true, false
         */
        var float2BigDecimal: Boolean = DEFAULT_FLOAT_2_BIG_DECIMAL

        /**
         * description: Maps OpenAPI integer to Long instead of Int.
         * default: true
         * values: true, false
         */
        var integer2Long: Boolean = DEFAULT_INTEGER_2_LONG

        val Jackson: Serialization
            get() = Serialization.Jackson

        val KotlinX: Serialization
            get() = Serialization.KotlinX

        val Jakarta: Validation
            get() = Validation.Jakarta

        val JavaX: Validation
            get() = Validation.JavaX

        val None: Validation
            get() = Validation.None
    }

    /** Client generation options. */
    open class ClientExtension {
        /**
         * description: Base package for generated API classes.
         * default: "dev.openapi2kotlin.client"
         */
        var packageName: String = DEFAULT_CLIENT_PACKAGE_NAME

        /**
         * description: Target HTTP client library used by generated client API.
         * required: true
         * values: Ktor, RestClient
         */
        var library: ClientLibrary? = null

        /**
         * description: Uses the first OpenAPI server variable matching `basePathVar` when it has a default value, e.g.
         * ```
         * servers:
         *   - url: '/{basePath}/'
         *     variables:
         *       basePath:
         *         default: 'v5/'
         * ```
         * default: "basePath"
         */
        var basePathVar: String = DEFAULT_BASE_PATH_VAR

        /**
         * description: Singularizes method names for single-resource endpoints, e.g. `retrieveQuote`.
         * default: true
         * values: true, false
         */
        var methodNameSingularized: Boolean = true

        /**
         * description: Pluralizes method names for collection endpoints, e.g. `listQuotes`.
         * default: true
         * values: true, false
         */
        var methodNamePluralized: Boolean = true

        /**
         * description: Derives method names from OpenAPI `operationId` instead of URL path.
         * default: false
         * values: true, false
         */
        var methodNameFromOperationId: Boolean = false

        fun setLibrary(value: String?) {
            library = value?.let { ClientLibrary.fromValue(it) }
        }

        val Ktor: ClientLibrary
            get() = ClientLibrary.Ktor

        val RestClient: ClientLibrary
            get() = ClientLibrary.RestClient
    }

    /** Server generation options. */
    open class ServerExtension {
        /**
         * description: Base package for generated API classes.
         * default: "dev.openapi2kotlin.server"
         */
        var packageName: String = DEFAULT_SERVER_PACKAGE_NAME

        /**
         * description: Target server framework used by generated server API.
         * required: true
         * values: Ktor, Spring
         */
        var library: ServerLibrary? = null

        /**
         * description: Enables generated Swagger/OpenAPI annotations.
         * default: Ktor -> false, Spring -> true
         * values: true, false
         */
        var swagger: Boolean? = null

        /**
         * description: Uses the first OpenAPI server variable matching `basePathVar` when it has a default value, e.g.
         * ```
         * servers:
         *   - url: '/{basePath}/'
         *     variables:
         *       basePath:
         *         default: 'v5/'
         * ```
         * default: "basePath"
         */
        var basePathVar: String = DEFAULT_BASE_PATH_VAR

        /**
         * description: Singularizes method names for single-resource endpoints, e.g. `retrieveQuote`.
         * default: true
         * values: true, false
         */
        var methodNameSingularized: Boolean = true

        /**
         * description: Pluralizes method names for collection endpoints, e.g. `listQuotes`.
         * default: true
         * values: true, false
         */
        var methodNamePluralized: Boolean = true

        /**
         * description: Derives method names from OpenAPI `operationId` instead of URL path.
         * default: false
         * values: true, false
         */
        var methodNameFromOperationId: Boolean = false

        fun setLibrary(value: String?) {
            library = value?.let { ServerLibrary.fromValue(it) }
        }

        val Ktor: ServerLibrary
            get() = ServerLibrary.Ktor

        val Spring: ServerLibrary
            get() = ServerLibrary.Spring
    }

    enum class Serialization {
        Jackson,
        KotlinX;

        internal fun toUseCaseSerialization(): OpenApi2KotlinUseCase.ModelConfig.Serialization = when (this) {
            Jackson -> OpenApi2KotlinUseCase.ModelConfig.Serialization.JACKSON
            KotlinX -> OpenApi2KotlinUseCase.ModelConfig.Serialization.KOTLINX
        }
    }

    enum class Validation {
        None,
        Jakarta,
        JavaX;

        internal fun toUseCaseValidation(): OpenApi2KotlinUseCase.ModelConfig.Validation? = when (this) {
            None -> null
            Jakarta -> OpenApi2KotlinUseCase.ModelConfig.Validation.JAKARTA
            JavaX -> OpenApi2KotlinUseCase.ModelConfig.Validation.JAVAX
        }
    }

    enum class ClientLibrary(
        val value: String,
    ) {
        Ktor("Ktor"),
        RestClient("RestClient"),
        ;

        companion object {
            fun fromValue(value: String): ClientLibrary =
                ClientLibrary.entries.firstOrNull {
                    it.value.equals(value, ignoreCase = true)
                } ?: throw IllegalArgumentException(
                    "Unexpected ClientLibrary value: '$value'"
                )
        }
    }

    enum class ServerLibrary(
        val value: String,
    ) {
        Ktor("Ktor"),
        Spring("Spring"),
        ;

        companion object {
            fun fromValue(value: String): ServerLibrary =
                ServerLibrary.entries.firstOrNull {
                    it.value.equals(value, ignoreCase = true)
                } ?: throw IllegalArgumentException(
                    "Unexpected ServerLibrary value: '$value'"
                )
        }
    }

    companion object {
        const val DEFAULT_PACKAGE_NAME = "dev.openapi2kotlin"
        const val DEFAULT_MODEL_PACKAGE_NAME = "$DEFAULT_PACKAGE_NAME.model"
        const val DEFAULT_CLIENT_PACKAGE_NAME = "$DEFAULT_PACKAGE_NAME.client"
        const val DEFAULT_SERVER_PACKAGE_NAME = "$DEFAULT_PACKAGE_NAME.server"
        const val DEFAULT_BASE_PATH_VAR = "basePath"

        const val DEFAULT_DOUBLE_2_BIG_DECIMAL = false
        const val DEFAULT_FLOAT_2_BIG_DECIMAL = false
        const val DEFAULT_INTEGER_2_LONG = true
    }
}
