package dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.client

import dev.openapi2kotlin.application.core.openapi2kotlin.model.client.ClientApiDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.raw.RawPathDO

fun prepareClientApis(rawPaths: List<RawPathDO>): List<ClientApiDO> {
    // todo implement
    return rawPaths.map {
        ClientApiDO(rawPath = it)
    }
}