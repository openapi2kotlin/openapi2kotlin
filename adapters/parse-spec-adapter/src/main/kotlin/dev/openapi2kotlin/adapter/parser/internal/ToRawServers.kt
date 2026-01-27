package dev.openapi2kotlin.adapter.parser.internal

import dev.openapi2kotlin.application.core.openapi2kotlin.model.raw.RawServerDO
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.servers.Server
import io.swagger.v3.oas.models.servers.ServerVariable

internal fun OpenAPI.toRawServers(): List<RawServerDO> =
    servers.orEmpty()
        .mapNotNull { it?.toRawServerDO() }

private fun Server.toRawServerDO(): RawServerDO =
    RawServerDO(
        url = url,
        vars = variables
            ?.entries
            ?.mapNotNull { (name, variable) ->
                variable?.toRawVar(name)
            }
            .orEmpty()
            .sortedBy { it.name },
    )

private fun ServerVariable.toRawVar(name: String): RawServerDO.Var? {
    val defaultValue = default ?: return null

    return RawServerDO.Var(
        name = name,
        defaultValue = defaultValue,
    )
}
