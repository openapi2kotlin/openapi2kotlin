package dev.openapi2kotlin.application.core.openapi2kotlin.port

import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.ModelDO
import java.nio.file.Path

fun interface GenerateModelPort {
    fun generateModel(command: Command)

    data class Command(
        val models: List<ModelDO>,
        val outputDirPath: Path,
    )
}