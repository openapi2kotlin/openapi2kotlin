package dev.openapi2kotlin.tools.generatortools

import java.io.File

fun File.postProcess() {
    walkTopDown()
        .filter { it.isFile && it.extension == "kt" }
        .forEach { file ->
            val text = file.readText()
            file.writeText(
                text
                    // KotlinPoet add redundant public modifiers by default
                    .replace("public ", "")
                    // KotlinPoet escapes this package segment; we prefer the normal form.
                    .replace(".`annotation`.", ".annotation.")
                    // KotlinPoet emits invalid bare imports for aliased annotation symbols in the default package.
                    .replace(Regex("""(?m)^import\s+[A-Z][A-Za-z0-9_]*\s*$\n?"""), ""),
            )
            file.writeText(
                file.readText()
                    .withSortedImports(),
            )
        }
}

private fun String.withSortedImports(): String {
    val lines = lines()
    val firstImportIndex = lines.indexOfFirst { it.startsWith("import ") }
    val lastImportIndex =
        lines.indexOfLast { it.startsWith("import ") }
    val hasImportBlock = firstImportIndex != -1 && lastImportIndex >= firstImportIndex

    return if (!hasImportBlock) {
        this
    } else {
        val sortedImports =
            lines
                .subList(firstImportIndex, lastImportIndex + 1)
                .sortedWith(
                    compareBy<String> { importGroup(it) }
                        .thenBy { it.removePrefix("import ").substringBefore(" as ") }
                        .thenBy { it.substringAfter(" as ", missingDelimiterValue = "") },
                )

        buildString {
            append(lines.subList(0, firstImportIndex).joinToString("\n"))
            append('\n')
            append(sortedImports.joinToString("\n"))
            if (lastImportIndex + 1 < lines.size) {
                append('\n')
                append(lines.subList(lastImportIndex + 1, lines.size).joinToString("\n"))
            }
        }
    }
}

private fun importGroup(line: String): Int {
    val importBody = line.removePrefix("import ")
    return when {
        " as " in importBody -> 2
        importBody.startsWith("java.") || importBody.startsWith("javax.") || importBody.startsWith("kotlin.") -> 1
        else -> 0
    }
}
