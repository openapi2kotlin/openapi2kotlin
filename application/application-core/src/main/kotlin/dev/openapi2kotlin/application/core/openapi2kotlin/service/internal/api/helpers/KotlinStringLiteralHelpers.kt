package dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.api.helpers

internal fun String.toKotlinStringLiteral(): String =
    buildString {
        append('"')
        for (ch in this@toKotlinStringLiteral) {
            when (ch) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(ch)
            }
        }
        append('"')
    }
