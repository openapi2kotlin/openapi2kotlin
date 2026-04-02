package dev.openapi2kotlin.adapter.generateserver.http4k.internal

import dev.openapi2kotlin.application.core.openapi2kotlin.domain.api.ApiEndpointDO

internal fun List<ApiEndpointDO>.sortedByHttp4kRouteSpecificity(): List<ApiEndpointDO> =
    sortedWith(
        compareBy<ApiEndpointDO> { pathParamCount(it) }
            .thenByDescending { staticSegmentCount(it) },
    )

private fun pathParamCount(endpoint: ApiEndpointDO): Int =
    endpoint.rawOperation.path
        .split("/")
        .count { it.startsWith("{") && it.endsWith("}") }

private fun staticSegmentCount(endpoint: ApiEndpointDO): Int =
    endpoint.rawOperation.path
        .split("/")
        .count { it.isNotBlank() && !(it.startsWith("{") && it.endsWith("}")) }
