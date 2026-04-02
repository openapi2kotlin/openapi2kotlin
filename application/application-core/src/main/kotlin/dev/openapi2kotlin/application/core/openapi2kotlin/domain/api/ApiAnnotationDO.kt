package dev.openapi2kotlin.application.core.openapi2kotlin.domain.api

data class ApiAnnotationDO(
    val fqName: String,
    val argsCode: List<String> = emptyList(),
)
