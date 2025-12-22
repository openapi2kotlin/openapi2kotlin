package dev.openapi2kotlin.application.core.openapi2kotlin.port

import dev.openapi2kotlin.application.core.openapi2kotlin.model.client.ClientApiDO
import java.nio.file.Path

fun interface GenerateClientPort {
    fun generateClient(command: Command)

    data class Command(
        val clientApis: List<ClientApiDO>,
        val clientPackageName: String,
        val modelPackageName: String,
        val outputDirPath: Path,
    )
}