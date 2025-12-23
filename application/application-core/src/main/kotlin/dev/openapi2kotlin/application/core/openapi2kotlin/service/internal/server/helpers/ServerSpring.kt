package dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.server.helpers

import dev.openapi2kotlin.application.core.openapi2kotlin.model.server.ServerAnnotationDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.server.ServerApiDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.server.ServerEndpointDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.server.ServerParamDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.server.ServerRequestBodyDO
import dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.server.ServerPrepareContext

internal fun List<ServerApiDO>.enrichForSpring(ctx: ServerPrepareContext): List<ServerApiDO> =
    map { api ->
        api.copy(
            annotations = api.annotations + listOf(
                ServerAnnotationDO("org.springframework.validation.annotation.Validated"),
                // todo
            ),
            endpoints = api.endpoints.map { it.enrichEndpointForSpring() },
        )
    }

private fun ServerEndpointDO.enrichEndpointForSpring(): ServerEndpointDO {
    val anns = annotations + listOf(
        // todo
    )
    return copy(
        annotations = anns,
        params = params.map { it.enrichParamForSpring() },
        requestBody = requestBody?.enrichRequestBodyForSpring(),
    )
}

private fun ServerParamDO.enrichParamForSpring(): ServerParamDO =
    copy(
        annotations = annotations + listOf(
            // todo
        )
    )

private fun ServerRequestBodyDO.enrichRequestBodyForSpring(): ServerRequestBodyDO =
    copy(
        annotations = annotations + listOf(
            // todo
        )
    )
