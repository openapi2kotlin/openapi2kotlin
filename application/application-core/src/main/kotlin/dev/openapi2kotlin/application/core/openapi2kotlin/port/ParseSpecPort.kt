package dev.openapi2kotlin.application.core.openapi2kotlin.port

import dev.openapi2kotlin.application.core.openapi2kotlin.domain.raw.RawPathDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.raw.RawSchemaDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.raw.RawServerDO
import java.nio.file.Path

fun interface ParseSpecPort {
    fun parseSpec(specPath: Path): RawOpenApiDO

    data class RawOpenApiDO(
        val rawSchemas: List<RawSchemaDO>,
        val rawPaths: List<RawPathDO>,
        val rawServers: List<RawServerDO>,
    )
}
