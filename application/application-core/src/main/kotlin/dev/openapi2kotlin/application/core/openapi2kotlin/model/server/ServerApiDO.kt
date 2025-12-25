package dev.openapi2kotlin.application.core.openapi2kotlin.model.server

import dev.openapi2kotlin.application.core.openapi2kotlin.model.raw.RawPathDO

data class ServerApiDO (
    val rawPath: RawPathDO,
    val generatedName: String,
    val endpoints: List<ServerEndpointDO>,
    val annotations: List<ServerAnnotationDO>,
    var route: ServerRouteDO? = null,
)