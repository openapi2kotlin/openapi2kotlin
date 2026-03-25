package dev.openapi2kotlin.tools.apigenerator.internal

import java.io.File

private const val KOTLIN_INDENT = "    "

private data class FunLineParts(
    val before: String,
    val inside: String,
    val after: String,
)

private class TopLevelScanner {
    private var paren = 0
    private var bracket = 0
    private var angle = 0
    private var inString = false
    private var stringQuote = '"'
    private var escape = false

    fun process(ch: Char): Boolean {
        if (inString) {
            processStringChar(ch)
            return false
        }

        when (ch) {
            '"', '\'' -> {
                inString = true
                stringQuote = ch
            }
            '(' -> paren++
            ')' -> decreaseIfNeeded { paren-- }
            '[' -> bracket++
            ']' -> decreaseIfNeeded { bracket-- }
            '<' -> angle++
            '>' -> decreaseIfNeeded { angle-- }
        }

        return isAtTopLevel()
    }

    private fun processStringChar(ch: Char) {
        if (escape) {
            escape = false
            return
        }

        when (ch) {
            '\\' -> escape = true
            stringQuote -> inString = false
        }
    }

    private fun isAtTopLevel(): Boolean = paren == 0 && bracket == 0 && angle == 0 && !inString

    private inline fun decreaseIfNeeded(update: () -> Unit) {
        if (isAtTopLevel()) return
        update()
    }
}

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
        out += line.rewriteLineIfNeeded()
    }

    return out.joinToString("\n")
}

private fun String.rewriteLineIfNeeded(): List<String> {
    val parts = parseFunLine() ?: return listOf(this)
    val rewritten =
        if (hasTopLevelComma(parts.inside)) {
            val params =
                splitTopLevelCommas(parts.inside)
                    .map { it.trim() }
                    .filter { it.isNotBlank() }

            if (params.size <= 1) {
                null
            } else {
                val indent = parts.before.takeWhile { it.isWhitespace() }
                val paramIndent = indent + KOTLIN_INDENT

                buildList<String> {
                    add("${parts.before}(")
                    params.forEach { param ->
                        add("$paramIndent$param,")
                    }
                    add("$indent)${parts.after}")
                }
            }
        } else {
            null
        }

    return rewritten ?: listOf(this)
}

private fun String.parseFunLine(): FunLineParts? {
    val funIdx = indexOf("fun ")
    return if (funIdx == -1) {
        null
    } else {
        val openIdx = indexOf('(', startIndex = funIdx)
        val closeIdx = lastIndexOf(')')

        if (openIdx == -1 || closeIdx == -1 || closeIdx < openIdx) {
            null
        } else {
            FunLineParts(
                before = substring(0, openIdx),
                inside = substring(openIdx + 1, closeIdx),
                after = substring(closeIdx + 1),
            )
        }
    }
}

private fun hasTopLevelComma(s: String): Boolean {
    val scanner = TopLevelScanner()
    for (ch in s) {
        val atTopLevel = scanner.process(ch)
        if (ch == ',' && atTopLevel) {
            return true
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
    val scanner = TopLevelScanner()

    for (ch in s) {
        val atTopLevel = scanner.process(ch)
        if (ch == ',' && atTopLevel) {
            result += buf.toString()
            buf.setLength(0)
        } else {
            buf.append(ch)
        }
    }

    result += buf.toString()
    return result
}
