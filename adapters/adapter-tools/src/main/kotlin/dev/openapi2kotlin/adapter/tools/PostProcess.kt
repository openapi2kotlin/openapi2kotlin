package dev.openapi2kotlin.adapter.tools

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
            )
        }
}
