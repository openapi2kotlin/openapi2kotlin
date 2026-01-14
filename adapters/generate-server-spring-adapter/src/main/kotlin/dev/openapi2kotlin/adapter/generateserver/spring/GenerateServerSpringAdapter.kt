package dev.openapi2kotlin.adapter.generateserver.spring

import dev.openapi2kotlin.adapter.generateserver.spring.internal.applySpringMvcAnnotations
import dev.openapi2kotlin.application.core.openapi2kotlin.port.GenerateApiPort
import dev.openapi2kotlin.tools.apigenerator.ApiGenerator


class GenerateServerSpringAdapter : GenerateApiPort {
    override fun generateApi(command: GenerateApiPort.Command) {
        command.apis.applySpringMvcAnnotations()
        ApiGenerator(SpringApiPolicy).generateApi(command)
    }
}