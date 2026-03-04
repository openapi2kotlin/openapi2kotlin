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
    /** Enables or disables code generation for the current Gradle run. */
    var enabled: Boolean = true

    /** Path to the OpenAPI specification file. */
    var inputSpec: String? = null

    /** Output directory for generated Kotlin sources. */
    var outputDir: String? = null

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

    internal fun toUseCaseModelConfig(): OpenApi2KotlinUseCase.ModelConfig {
        val effectiveSerialization = model.serialization?.toUseCaseSerialization()
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
        /** Base package for generated model classes. */
        var packageName: String = DEFAULT_MODEL_PACKAGE_NAME

        /**
         * Serialization annotation family for generated models.
         *
         * - `Jackson` emits Jackson annotations.
         * - `KotlinX` emits kotlinx serialization annotations.
         * - `null` emits no serialization annotations.
         */
        var serialization: Serialization? = DEFAULT_SERIALIZATION

        /**
         * Validation annotation namespace for generated models.
         *
         * - `Jakarta` emits `jakarta.validation.*`.
         * - `JavaX` emits `javax.validation.*`.
         * - `null` disables validation annotations.
         */
        var validation: Validation? = null

        /** Maps OpenAPI `number/double` to `BigDecimal` instead of `Double`. */
        var double2BigDecimal: Boolean = DEFAULT_DOUBLE_2_BIG_DECIMAL

        /** Maps OpenAPI `number/float` to `BigDecimal` instead of `Float`. */
        var float2BigDecimal: Boolean = DEFAULT_FLOAT_2_BIG_DECIMAL

        /** Maps OpenAPI `integer` to `Long` instead of `Int`. */
        var integer2Long: Boolean = DEFAULT_INTEGER_2_LONG

        val Jackson: Serialization
            get() = Serialization.Jackson

        val KotlinX: Serialization
            get() = Serialization.KotlinX

        val Jakarta: Validation
            get() = Validation.Jakarta

        val JavaX: Validation
            get() = Validation.JavaX
    }

    /** Shared API package configuration. */
    open class ApiBaseExtension {
        /** Base package for generated API classes. */
        var packageName: String = DEFAULT_PACKAGE_NAME

        /** Name of the generated base-path variable. */
        var basePathVar: String = DEFAULT_BASE_PATH_VAR
    }

    /** Client generation options. */
    open class ClientExtension : ApiBaseExtension() {
        /** Target client library. Required when `client {}` is used. */
        var library: ClientLibrary? = null

        fun setLibrary(value: String?) {
            library = value?.let { ClientLibrary.fromValue(it) }
        }

        val Ktor: ClientLibrary
            get() = ClientLibrary.Ktor

        val RestClient: ClientLibrary
            get() = ClientLibrary.RestClient

        init {
            packageName = DEFAULT_CLIENT_PACKAGE_NAME
        }
    }

    /** Server generation options. */
    open class ServerExtension : ApiBaseExtension() {
        /** Target server library. Required when `server {}` is used. */
        var library: ServerLibrary? = null

        /**
         * Enables or disables generated Swagger/OpenAPI annotations.
         *
         * If omitted, Spring defaults to `true` and Ktor defaults to `false`.
         */
        var swagger: Boolean? = null

        fun setLibrary(value: String?) {
            library = value?.let { ServerLibrary.fromValue(it) }
        }

        val Ktor: ServerLibrary
            get() = ServerLibrary.Ktor

        val Spring: ServerLibrary
            get() = ServerLibrary.Spring

        init {
            packageName = DEFAULT_SERVER_PACKAGE_NAME
        }
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
        Jakarta,
        JavaX;

        internal fun toUseCaseValidation(): OpenApi2KotlinUseCase.ModelConfig.Validation = when (this) {
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
        val DEFAULT_SERIALIZATION = Serialization.KotlinX

        const val DEFAULT_DOUBLE_2_BIG_DECIMAL = false
        const val DEFAULT_FLOAT_2_BIG_DECIMAL = false
        const val DEFAULT_INTEGER_2_LONG = true
    }
}
