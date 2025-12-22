package dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.model.helpers

/**
 * Convert OpenAPI property name to Kotlin identifier.
 *
 * Current rules:
 * - "@type" -> "atType"
 */
internal fun String.toKotlinName(): String =
    if (startsWith("@") && length > 1) {
        val rest = substring(1)
        "at" + rest.replaceFirstChar { it.uppercaseChar() }
    } else {
        this
    }