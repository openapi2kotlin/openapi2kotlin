package dev.openapi2kotlin.application.core.openapi2kotlin.model.api

import dev.openapi2kotlin.application.core.openapi2kotlin.model.raw.RawPathDO

data class ApiEndpointDO(
    val rawOperation: RawPathDO.OperationDO,
    val generatedName: String,
    val params: List<ApiParamDO>,
    val requestBody: ApiRequestBodyDO?,
    val successResponse: ApiSuccessResponseDO?,
    val annotations: List<ApiAnnotationDO>,
)