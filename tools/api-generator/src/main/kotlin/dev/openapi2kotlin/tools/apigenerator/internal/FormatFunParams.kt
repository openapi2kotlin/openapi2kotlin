package dev.openapi2kotlin.tools.apigenerator.internal

import java.io.File

internal fun File.formatFunParams() {
    walkTopDown()
        .filter { it.isFile && it.extension == "kt" }
        .forEach { file ->
            val original = file.readText()
            val updated = original.rewriteSingleLineFunParamLists()
            if (updated != original) file.writeText(updated)
        }
}

private fun String.rewriteSingleLineFunParamLists(): String {
    val lines = this.lines()
    val out = ArrayList<String>(lines.size)

    for (line in lines) {
        val funIdx = line.indexOf("fun ")
        if (funIdx == -1) {
            out += line
            continue
        }

        val openIdx = line.indexOf('(', startIndex = funIdx)
        val closeIdx = line.lastIndexOf(')')
        if (openIdx == -1 || closeIdx == -1 || closeIdx < openIdx) {
            out += line
            continue
        }

        val before = line.substring(0, openIdx) // includes indentation + "fun name"
        val inside = line.substring(openIdx + 1, closeIdx)
        val after = line.substring(closeIdx + 1)

        // Only rewrite if we have at least two top-level params (comma at top level)
        if (!hasTopLevelComma(inside)) {
            out += line
            continue
        }

        val params = splitTopLevelCommas(inside)
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (params.size <= 1) {
            out += line
            continue
        }

        val indent = before.takeWhile { it.isWhitespace() }
        val paramIndent = indent + "  "

        out += "$before("
        params.forEach { p ->
            out += "$paramIndent$p,"
        }
        out += "$indent)$after"
    }

    return out.joinToString("\n")
}

private fun hasTopLevelComma(s: String): Boolean {
    var paren = 0
    var bracket = 0
    var angle = 0

    var inString = false
    var stringQuote = '"'
    var escape = false

    for (ch in s) {
        if (inString) {
            if (escape) {
                escape = false
                continue
            }
            when (ch) {
                '\\' -> escape = true
                stringQuote -> inString = false
            }
            continue
        }

        when (ch) {
            '"', '\'' -> { inString = true; stringQuote = ch }
            '(' -> paren++
            ')' -> if (paren > 0) paren--
            '[' -> bracket++
            ']' -> if (bracket > 0) bracket--
            '<' -> angle++
            '>' -> if (angle > 0) angle--
            ',' -> if (paren == 0 && bracket == 0 && angle == 0) return true
        }
    }
    return false
}

/**
 * Splits a parameter list by commas that are at top level (not nested in (), [], <> or strings).
 */
private fun splitTopLevelCommas(s: String): List<String> {
    val result = mutableListOf<String>()
    val buf = StringBuilder()

    var paren = 0
    var bracket = 0
    var angle = 0

    var inString = false
    var stringQuote: Char = '"'
    var escape = false

    for (ch in s) {
        if (inString) {
            buf.append(ch)
            if (escape) {
                escape = false
            } else {
                when (ch) {
                    '\\' -> escape = true
                    stringQuote -> inString = false
                }
            }
            continue
        }

        when (ch) {
            '"', '\'' -> {
                inString = true
                stringQuote = ch
                buf.append(ch)
            }

            '(' -> { paren++; buf.append(ch) }
            ')' -> { if (paren > 0) paren--; buf.append(ch) }

            '[' -> { bracket++; buf.append(ch) }
            ']' -> { if (bracket > 0) bracket--; buf.append(ch) }

            '<' -> { angle++; buf.append(ch) }
            '>' -> { if (angle > 0) angle--; buf.append(ch) }

            ',' -> {
                if (paren == 0 && bracket == 0 && angle == 0) {
                    result += buf.toString()
                    buf.setLength(0)
                } else {
                    buf.append(ch)
                }
            }

            else -> buf.append(ch)
        }
    }

    result += buf.toString()
    return result
}