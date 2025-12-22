package dev.openapi2kotlin.adapter.parser

import dev.openapi2kotlin.adapter.parser.internal.toRawPaths
import dev.openapi2kotlin.adapter.parser.internal.toRawSchemas
import dev.openapi2kotlin.application.core.openapi2kotlin.port.ParseSpecPort
import io.swagger.v3.parser.OpenAPIV3Parser
import java.nio.file.Path

class ParseSpecAdapter: ParseSpecPort {
    override fun parseSpec(specPath: Path): ParseSpecPort.RawOpenApiDO {
        val openApi = OpenAPIV3Parser()
            .read(specPath.toAbsolutePath().toString())

        return ParseSpecPort.RawOpenApiDO(
            rawSchemas = openApi.toRawSchemas(),
            rawPaths = openApi.toRawPaths()
        )
    }
}