package dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.model.helpers

internal fun cleanSchemaNameHandler(name: String): String =
    name.replace("_", "")