package dev.openapi2kotlin.gradleplugin

open class OpenApi2KotlinExtension {
    var inputSpec: String? = null
    var outputDir: String? = null

    var srcDirEnabled: Boolean = true

    val model = ModelConfigExtension()
    val client = ClientConfigExtension()
    val server = ServerConfigExtension()

    fun model(block: ModelConfigExtension.() -> Unit) = model.block()
    fun client(block: ClientConfigExtension.() -> Unit) = client.block()
    fun server(block: ServerConfigExtension.() -> Unit) = server.block()

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
        var enabled: Boolean? = null
        var packageName: String? = null
    }

    open class ServerConfigExtension {
        var enabled: Boolean? = null
        var packageName: String? = null
        var framework: String? = null
    }
}
