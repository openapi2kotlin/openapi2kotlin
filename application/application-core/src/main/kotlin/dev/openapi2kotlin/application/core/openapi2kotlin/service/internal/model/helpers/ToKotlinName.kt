package dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.model.helpers

/**
 * Convert OpenAPI property name to Kotlin identifier.
 *
 * Examples:
 * - "@type" -> "atType"
 * - "Id" -> "id"
 * - "CategoryId" -> "categoryId"
 * - "EAN" -> "ean"
 */
internal fun String.toKotlinName(): String =
    when {
        startsWith("@") && length > 1 ->
            listOf("at") + substring(1).splitIdentifierWords()

        else ->
            splitIdentifierWords()
    }
        .toLowerCamelPropertyName()
        .ensureKotlinIdentifier(defaultName = "value")

private fun List<String>.toLowerCamelPropertyName(): String =
    firstOrNull()?.lowercase().orEmpty() +
        drop(1).joinToString("") { token ->
            if (token.length <= 2 && token.all(Char::isUpperCase)) token
            else token.lowercase().replaceFirstChar(Char::uppercaseChar)
        }

private fun String.splitIdentifierWords(): List<String> {
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

        val prev = getOrNull(i - 1)
        val next = getOrNull(i + 1)

        val prevIsLower = prev?.isLowerCase() == true
        val prevIsUpper = prev?.isUpperCase() == true
        val prevIsDigit = prev?.isDigit() == true
        val prevIsLetter = prev?.isLetter() == true

        val cIsUpper = c.isUpperCase()
        val cIsLower = c.isLowerCase()

        val nextIsLower = next?.isLowerCase() == true

        val boundary =
            (prev != null) && (
                (prevIsLower && cIsUpper) ||
                    (prevIsUpper && cIsUpper && nextIsLower) ||
                    (prevIsLetter && c.isDigit()) ||
                    (prevIsDigit && (cIsUpper || cIsLower))
                )

        if (boundary) flush()
        current.append(c)
    }

    flush()
    return tokens.filter { it.isNotBlank() }
}

private fun String.ensureKotlinIdentifier(defaultName: String): String {
    val normalized = replace("[^A-Za-z0-9_]".toRegex(), "_")
        .replace(Regex("_+"), "_")
        .trim('_')
        .ifBlank { defaultName }
        .let { if (it.first().isDigit()) "_$it" else it }

    return if (normalized in kotlinKeywords) "${normalized}_" else normalized
}

private val kotlinKeywords = setOf(
    "as",
    "break",
    "class",
    "continue",
    "do",
    "else",
    "false",
    "for",
    "fun",
    "if",
    "in",
    "interface",
    "is",
    "null",
    "object",
    "package",
    "return",
    "super",
    "this",
    "throw",
    "true",
    "try",
    "typealias",
    "val",
    "var",
    "when",
    "while",
)
