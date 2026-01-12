package dev.openapi2kotlin.application.core.openapi2kotlin.model.api

data class ApiAnnotationDO (
    val fqName: String,
    val argsCode: List<String> = emptyList(),
)