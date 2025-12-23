package dev.openapi2kotlin.application.core.openapi2kotlin.model.server

import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.FieldTypeDO

data class ServerRequestBodyDO(
    val generatedName: String,
    val type: FieldTypeDO,
    val annotations: List<ServerAnnotationDO>,
)
