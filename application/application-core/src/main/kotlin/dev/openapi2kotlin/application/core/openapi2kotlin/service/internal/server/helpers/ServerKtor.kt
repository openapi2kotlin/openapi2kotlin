package dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.server.helpers

import dev.openapi2kotlin.application.core.openapi2kotlin.model.server.ServerApiDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.server.ServerRouteDO
import dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.server.ServerPrepareContext

internal fun List<ServerApiDO>.enrichForKtor(ctx: ServerPrepareContext): List<ServerApiDO> =
    map { api ->
        val basePath = inferBasePath(api)

        val apiStem = api.generatedName.removeSuffix("Api").ifBlank { api.generatedName }
        val funName = apiStem.replaceFirstChar { it.lowercaseChar() } + "Routes"

        api.copy(
            route = ServerRouteDO(
                generatedFileName = apiStem + "Routes",
                generatedFunName = funName,
                basePath = basePath,
            )
        )
    }

private fun inferBasePath(api: ServerApiDO): String {
    // Best-effort: choose the shortest concrete path and take its first segment.
    val shortest = api.rawPath.operations
        .map { it.path.trim() }
        .filter { it.isNotBlank() }
        .minByOrNull { it.length }
        ?: "/"

    val seg = shortest.trim('/').split('/').firstOrNull().orEmpty()
    return "/" + seg
}
