package dev.openapi2kotlin.gradleplugin

import org.gradle.api.GradleException

open class OpenApi2KotlinExtension {
    var enabled: Boolean = true
    var inputSpec: String? = null
    var outputDir: String? = null

    var srcDirEnabled: Boolean = true

    val model = ModelConfigExtension()

    var client: ClientExtension? = null
        private set
    var server: ServerExtension? = null
        private set

    fun model(block: ModelConfigExtension.() -> Unit) = model.block()

    fun client(block: ClientExtension.() -> Unit) {
        if (server != null) throwClientServerExclusivityError()
        val draft = (client ?: ClientExtension()).apply(block)
        client = resolveClientExtension(draft)
    }

    fun server(block: ServerExtension.() -> Unit) {
        if (client != null) throwClientServerExclusivityError()
        val draft = (server ?: ServerExtension()).apply(block)
        server = resolveServerExtension(draft)
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

    private fun resolveClientExtension(draft: ClientExtension): ClientExtension {
        val library = draft.library ?: return draft
        val resolved: ClientExtension = when (library) {
            ClientLibrary.Ktor -> ClientKtorExtension()
            ClientLibrary.RestClient -> ClientRestClientExtension()
        }
        resolved.copyFrom(draft)
        resolved.library = library
        return resolved
    }

    private fun resolveServerExtension(draft: ServerExtension): ServerExtension {
        val library = draft.library ?: return draft
        val resolved: ServerExtension = when (library) {
            ServerLibrary.Ktor -> ServerKtorExtension()
            ServerLibrary.Spring -> ServerSpringExtension()
        }
        resolved.copyFrom(draft)
        resolved.library = library
        resolved.swagger.enabled = draft.swagger.enabled
        return resolved
    }

    open class ModelConfigExtension {
        var packageName: String? = null

        val annotations = ModelAnnotationsConfigExtension()
        val mapping = MappingConfigExtension()

        fun annotations(block: ModelAnnotationsConfigExtension.() -> Unit) = annotations.block()
        fun mapping(block: MappingConfigExtension.() -> Unit) = mapping.block()

        open class ModelAnnotationsConfigExtension {
            val jackson = JacksonConfigExtension()
            val validations = ValidationAnnotationsConfigExtension()

            fun jackson(block: JacksonConfigExtension.() -> Unit) = jackson.block()
            fun validations(block: ValidationAnnotationsConfigExtension.() -> Unit) = validations.block()

            open class JacksonConfigExtension {
                var enabled: Boolean? = null
                var jsonPropertyMapping: Boolean? = null
                var defaultDiscriminatorValue: Boolean? = null
                var strictDiscriminatorSerialization: Boolean? = null
                var jsonValue: Boolean? = null
                var jsonCreator: Boolean? = null
            }

            open class ValidationAnnotationsConfigExtension {
                var enabled: Boolean? = null
                var namespace: String? = null
            }
        }

        open class MappingConfigExtension {
            var double2BigDecimal: Boolean? = null
            var float2BigDecimal: Boolean? = null
            var integer2Long: Boolean? = null
        }
    }

    open class ApiBaseExtension {
        var packageName: String? = null
        var basePathVar: String? = null
    }

    private fun ApiBaseExtension.copyFrom(from: ApiBaseExtension) {
        packageName = from.packageName
        basePathVar = from.basePathVar
    }

    open class ClientExtension : ApiBaseExtension() {
        var library: ClientLibrary? = null

        fun setLibrary(value: String?) {
            library = value?.let { ClientLibrary.fromValue(it) }
        }

        val Ktor: ClientLibrary
            get() = ClientLibrary.Ktor

        val RestClient: ClientLibrary
            get() = ClientLibrary.RestClient
    }

    open class ServerExtension : ApiBaseExtension() {
        var library: ServerLibrary? = null
        val swagger = SwaggerConfigExtension()

        fun setLibrary(value: String?) {
            library = value?.let { ServerLibrary.fromValue(it) }
        }

        val Ktor: ServerLibrary
            get() = ServerLibrary.Ktor

        val Spring: ServerLibrary
            get() = ServerLibrary.Spring
    }

    open class ClientKtorExtension : ClientExtension()

    open class ClientRestClientExtension : ClientExtension()

    open class ServerKtorExtension : ServerExtension()

    open class ServerSpringExtension : ServerExtension()

    open class SwaggerConfigExtension {
        var enabled: Boolean? = null
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
}
