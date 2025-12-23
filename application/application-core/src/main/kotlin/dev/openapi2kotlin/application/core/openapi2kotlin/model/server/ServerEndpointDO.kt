package dev.openapi2kotlin.application.core.openapi2kotlin.model.server

import dev.openapi2kotlin.application.core.openapi2kotlin.model.raw.RawPathDO

data class ServerEndpointDO(
    val rawOperation: RawPathDO.OperationDO,
    val generatedName: String,
    val params: List<ServerParamDO>,
    val requestBody: ServerRequestBodyDO?,
    val successResponse: ServerSuccessResponseDO?,
    val annotations: List<ServerAnnotationDO>,
)