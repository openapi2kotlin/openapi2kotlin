package dev.openapi2kotlin.application.core.openapi2kotlin.model.server

data class ServerAnnotationDO (
    val fqName: String,
    val argsCode: List<String> = emptyList(),
)