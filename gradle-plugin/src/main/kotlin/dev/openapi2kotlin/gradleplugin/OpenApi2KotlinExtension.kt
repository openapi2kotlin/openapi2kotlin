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
    }

    open class ClientConfigExtension {
        var enabled: Boolean? = null
        var packageName: String? = null
    }

    open class ServerConfigExtension {
        var enabled: Boolean? = null
        var packageName: String? = null
    }
}