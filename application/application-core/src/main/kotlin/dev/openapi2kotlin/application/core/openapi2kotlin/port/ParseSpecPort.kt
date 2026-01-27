package dev.openapi2kotlin.application.core.openapi2kotlin.port

import dev.openapi2kotlin.application.core.openapi2kotlin.model.raw.RawPathDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.raw.RawSchemaDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.raw.RawServerDO
import java.nio.file.Path

fun interface ParseSpecPort {
    fun parseSpec(specPath: Path): RawOpenApiDO

    data class RawOpenApiDO(
        val rawSchemas: List<RawSchemaDO>,
        val rawPaths: List<RawPathDO>,
        val rawServers: List<RawServerDO>,
    )
}