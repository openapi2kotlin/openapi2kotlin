package dev.openapi2kotlin.adapter.generatemodel

import dev.openapi2kotlin.adapter.generatemodel.internal.generate
import dev.openapi2kotlin.application.core.openapi2kotlin.port.GenerateModelPort


class GenerateModelAdapter: GenerateModelPort {
    override fun generateModel(command: GenerateModelPort.Command) {
        generate(
            command.models,
            command.outputDirPath
        )
    }
}