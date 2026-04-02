package dev.openapi2kotlin.application.core.openapi2kotlin.domain.api

data class ApiContextDO(
    val apis: List<ApiDO>,
    val basePath: String,
)
