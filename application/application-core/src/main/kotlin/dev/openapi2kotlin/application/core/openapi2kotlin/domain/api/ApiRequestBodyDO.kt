package dev.openapi2kotlin.application.core.openapi2kotlin.domain.api

import dev.openapi2kotlin.application.core.openapi2kotlin.domain.model.FieldTypeDO

data class ApiRequestBodyDO(
    val generatedName: String,
    val type: FieldTypeDO,
    var annotations: List<ApiAnnotationDO> = emptyList(),
)
