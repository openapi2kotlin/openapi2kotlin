package dev.openapi2kotlin.adapter.generateclient.restclient

import dev.openapi2kotlin.application.core.openapi2kotlin.port.GenerateApiPort

class GenerateClientRestClientAdapter : GenerateApiPort {
    override fun generateApi(command: GenerateApiPort.Command) {
        RestClientInterfaceGenerator().generateApi(command)
        RestClientApiImplGenerator().generateApi(command)
    }
}
