package dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.api.helpers

private const val DEFAULT_IDENTIFIER_NAME = "value"

internal fun String.splitIdentifierWords(): List<String> {
    val tokens = mutableListOf<String>()
    val current = StringBuilder()

    fun flush() {
        if (current.isNotEmpty()) {
            tokens += current.toString()
            current.setLength(0)
        }
    }

    for (i in indices) {
        val c = this[i]
        if (!c.isLetterOrDigit()) {
            flush()
            continue
        }
        if (hasIdentifierBoundaryAt(i)) flush()
        current.append(c)
    }

    flush()
    return tokens.filter { it.isNotBlank() }
}

internal fun String.ensureKotlinIdentifier(defaultName: String = DEFAULT_IDENTIFIER_NAME): String =
    replace("[^A-Za-z0-9_]".toRegex(), "_")
        .replace(Regex("_+"), "_")
        .trim('_')
        .ifBlank { defaultName }
        .let { if (it.first().isDigit()) "_$it" else it }

private fun String.hasIdentifierBoundaryAt(index: Int): Boolean {
    val prev = getOrNull(index - 1) ?: return false
    val current = this[index]
    val next = getOrNull(index + 1)

    return (prev.isLowerCase() && current.isUpperCase()) ||
        (prev.isUpperCase() && current.isUpperCase() && next?.isLowerCase() == true) ||
        (prev.isLetter() && current.isDigit()) ||
        (prev.isDigit() && current.isLetter())
}
