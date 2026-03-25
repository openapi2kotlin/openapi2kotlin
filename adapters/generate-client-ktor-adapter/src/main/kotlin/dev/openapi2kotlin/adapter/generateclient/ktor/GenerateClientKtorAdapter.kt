package dev.openapi2kotlin.adapter.generateclient.ktor

import dev.openapi2kotlin.application.core.openapi2kotlin.port.GenerateApiPort
import dev.openapi2kotlin.tools.apigenerator.ApiGenerator

class GenerateClientKtorAdapter : GenerateApiPort {
    override fun generateApi(command: GenerateApiPort.Command) {
        ApiGenerator().generateApi(command)
        ApiImplGenerator().generateApi(command)
    }
}
