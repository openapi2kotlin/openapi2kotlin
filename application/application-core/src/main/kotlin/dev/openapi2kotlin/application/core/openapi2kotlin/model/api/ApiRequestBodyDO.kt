package dev.openapi2kotlin.application.core.openapi2kotlin.model.api

import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.FieldTypeDO

data class ApiRequestBodyDO(
    val generatedName: String,
    val type: FieldTypeDO,
    val annotations: List<ApiAnnotationDO>,
)
