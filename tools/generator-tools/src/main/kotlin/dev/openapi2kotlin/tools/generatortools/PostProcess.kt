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
                    .replace(Regex("""(?m)^import\s+[A-Z][A-Za-z0-9_]*\s*$\n?"""), "")
            )
        }
}
