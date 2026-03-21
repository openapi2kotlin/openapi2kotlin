package dev.openapi2kotlin.adapter.generateclient.http4k

import dev.openapi2kotlin.application.core.openapi2kotlin.port.GenerateApiPort

class GenerateClientHttp4kAdapter : GenerateApiPort {
    override fun generateApi(command: GenerateApiPort.Command) {
        Http4kClientInterfaceGenerator().generateApi(command)
        Http4kClientApiImplGenerator().generateApi(command)
    }
}
