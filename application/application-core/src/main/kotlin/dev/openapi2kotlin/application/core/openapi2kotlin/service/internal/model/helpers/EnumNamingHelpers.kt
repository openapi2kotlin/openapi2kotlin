package dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.model.helpers

import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.ModelDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.ModelShapeDO

internal fun String.hasIdentifierBoundaryAt(index: Int): Boolean {
    val prev = getOrNull(index - 1) ?: return false
    val current = this[index]
    val next = getOrNull(index + 1)

    return (prev.isLowerCase() && current.isUpperCase()) ||
        (prev.isUpperCase() && current.isUpperCase() && next?.isLowerCase() == true) ||
        (prev.isLetter() && current.isDigit()) ||
        (prev.isDigit() && current.isLetter())
}

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

internal fun String.ensureKotlinIdentifier(defaultName: String): String {
    val normalized =
        replace("[^A-Za-z0-9_]".toRegex(), "_")
            .replace(Regex("_+"), "_")
            .trim('_')
            .ifBlank { defaultName }
            .let { if (it.first().isDigit()) "_$it" else it }

    return if (normalized in modelKotlinKeywords) "${normalized}_" else normalized
}

private val modelKotlinKeywords =
    setOf(
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

internal fun List<String>.firstResolvableParentClass(byName: Map<String, ModelDO>): String? =
    firstNotNullOfOrNull { parentName ->
        when (byName[parentName]?.modelShape) {
            is ModelShapeDO.OpenClass,
            is ModelShapeDO.DataClass,
            is ModelShapeDO.EmptyClass,
            -> parentName
            else -> null
        }
    }
