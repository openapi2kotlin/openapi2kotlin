package dev.openapi2kotlin.adapter.parser

import dev.openapi2kotlin.adapter.parser.internal.toRawPaths
import dev.openapi2kotlin.adapter.parser.internal.toRawSchemas
import dev.openapi2kotlin.adapter.parser.internal.toRawServers
import dev.openapi2kotlin.application.core.openapi2kotlin.port.ParseSpecPort
import io.swagger.parser.OpenAPIParser
import io.swagger.v3.parser.core.models.ParseOptions
import java.nio.file.Path

class ParseSpecAdapter : ParseSpecPort {
    /**
     * Reads OpenAPI 3.x specs directly and converts Swagger 2.0 specs to OpenAPI 3 before mapping.
     */
    override fun parseSpec(specPath: Path): ParseSpecPort.RawOpenApiDO {
        val location = specPath.toAbsolutePath().toString()
        val result = OpenAPIParser().readLocation(location, null, ParseOptions().apply { isResolve = true })
        val openApi =
            checkNotNull(result.openAPI) {
                "Unable to parse OpenAPI/Swagger spec '$location': ${result.messages.orEmpty().joinToString("; ")}"
            }

        return ParseSpecPort.RawOpenApiDO(
            rawSchemas = openApi.toRawSchemas(),
            rawPaths = openApi.toRawPaths(),
            rawServers = openApi.toRawServers(),
        )
    }
}
