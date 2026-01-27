package dev.openapi2kotlin.application.core.openapi2kotlin.port

import dev.openapi2kotlin.application.core.openapi2kotlin.model.api.ApiContextDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.ModelDO
import java.nio.file.Path

fun interface GenerateApiPort {
    fun generateApi(command: Command)

    data class Command(
        val apiContext: ApiContextDO,
        val packageName: String,
        val modelPackageName: String,
        val outputDirPath: Path,
        val models: List<ModelDO>,
    )
}