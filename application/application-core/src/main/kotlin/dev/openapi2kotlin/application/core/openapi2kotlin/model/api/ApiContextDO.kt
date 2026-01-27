package dev.openapi2kotlin.application.core.openapi2kotlin.model.api

data class ApiContextDO (
    val apis: List<ApiDO>,
    val basePath: String,
)