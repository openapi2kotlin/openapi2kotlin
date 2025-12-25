package dev.openapi2kotlin.application.core.openapi2kotlin.model.server

data class ServerRouteDO (
    val generatedFileName: String,   // e.g. "CategoryRoutes"
    val generatedFunName: String,    // e.g. "categoryRoutes"
    val basePath: String,            // e.g. "/category"
)