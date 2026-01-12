package dev.openapi2kotlin.gradleplugin

import org.gradle.api.GradleException

open class OpenApi2KotlinExtension {
    var inputSpec: String? = null
    var outputDir: String? = null

    var srcDirEnabled: Boolean = true

    val model = ModelConfigExtension()

    var client: ClientConfigExtension? = null
        private set
    var server: ServerConfigExtension? = null
        private set

    fun model(block: ModelConfigExtension.() -> Unit) = model.block()

    fun client(block: ClientConfigExtension.() -> Unit) {
        if (server != null) {
            throwClientServerExclusivityError()
        }
        client = (client ?: ClientConfigExtension()).apply(block)
    }

    fun server(block: ServerConfigExtension.() -> Unit) {
        if (client != null) {
            throwClientServerExclusivityError()
        }
        server = (server ?: ServerConfigExtension()).apply(block)
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

    open class ModelConfigExtension {
        var packageName: String? = null

        val annotations = AnnotationsConfigExtension()
        val mapping = MappingConfigExtension()

        fun annotations(block: AnnotationsConfigExtension.() -> Unit) = annotations.block()
        fun mapping(block: MappingConfigExtension.() -> Unit) = mapping.block()

        open class AnnotationsConfigExtension {
            val jackson = JacksonConfigExtension()
            val validations = ValidationAnnotationsConfigExtension()
            val swagger = SwaggerConfigExtension()

            fun jackson(block: JacksonConfigExtension.() -> Unit) = jackson.block()
            fun validations(block: ValidationAnnotationsConfigExtension.() -> Unit) = validations.block()
            fun swagger(block: SwaggerConfigExtension.() -> Unit) = swagger.block()

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

            open class SwaggerConfigExtension {
                var enabled: Boolean? = null
            }
        }

        open class MappingConfigExtension {
            var double2BigDecimal: Boolean? = null
            var float2BigDecimal: Boolean? = null
            var integer2Long: Boolean? = null
        }
    }

    open class ClientConfigExtension {
        var packageName: String? = null
    }

    open class ServerConfigExtension {
        var packageName: String? = null
        var framework: String? = null
    }
}
