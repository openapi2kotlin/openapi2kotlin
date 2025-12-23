package dev.openapi2kotlin.application.core.openapi2kotlin.port

import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.ModelDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.server.ServerApiDO
import java.nio.file.Path

fun interface GenerateServerPort {
    fun generateServer(command: Command)

    data class Command(
        val serverApis: List<ServerApiDO>,
        val serverPackageName: String,
        val modelPackageName: String,
        val outputDirPath: Path,
        val models: List<ModelDO>,
    )
}