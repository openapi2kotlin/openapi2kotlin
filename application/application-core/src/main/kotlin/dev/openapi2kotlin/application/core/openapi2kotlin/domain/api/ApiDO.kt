package dev.openapi2kotlin.application.core.openapi2kotlin.domain.api

import dev.openapi2kotlin.application.core.openapi2kotlin.domain.raw.RawPathDO

data class ApiDO(
    val rawPath: RawPathDO,
    val generatedName: String,
    val endpoints: List<ApiEndpointDO>,
    var annotations: List<ApiAnnotationDO> = emptyList(),
)
