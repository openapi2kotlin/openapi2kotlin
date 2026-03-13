package dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.model.helpers

internal fun cleanSchemaNameHandler(name: String): String =
    name.substringAfterLast('.')
        .replace("_", "")
        .replace(Regex("[^A-Za-z0-9]"), "")
        .ifBlank { "Model" }
        .let { if (it.first().isDigit()) "_$it" else it }
