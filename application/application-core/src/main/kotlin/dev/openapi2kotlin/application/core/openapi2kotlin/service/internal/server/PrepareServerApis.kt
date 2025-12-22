package dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.server

import dev.openapi2kotlin.application.core.openapi2kotlin.model.raw.RawPathDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.server.ServerApiDO

fun prepareServerApis(rawPaths: List<RawPathDO>): List<ServerApiDO> {
    // todo implement
    return rawPaths.map {
        ServerApiDO(rawPath = it)
    }
}