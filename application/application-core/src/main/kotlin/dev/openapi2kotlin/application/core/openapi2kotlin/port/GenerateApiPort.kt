package dev.openapi2kotlin.application.core.openapi2kotlin.port

import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.ModelDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.api.ApiDO
import java.nio.file.Path

fun interface GenerateApiPort {
    fun generateApi(command: Command)

    data class Command(
        val apis: List<ApiDO>,
        val packageName: String,
        val modelPackageName: String,
        val outputDirPath: Path,
        val models: List<ModelDO>,
    )
}