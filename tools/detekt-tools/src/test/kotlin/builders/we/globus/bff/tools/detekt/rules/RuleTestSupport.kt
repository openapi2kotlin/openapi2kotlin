package builders.we.globus.bff.tools.detekt.rules

import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.test.lint
import dev.detekt.test.utils.compileContentForTest
import dev.detekt.test.utils.compileForTest
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

internal fun Rule.lintFile(
    fileName: String,
    code: String,
): List<Finding> = lint(compileContentForTest(code, fileName))

internal fun Rule.lintRepoFile(
    root: Path,
    relativePath: String,
    code: String,
): List<Finding> {
    root.resolve("settings.gradle.kts").writeText("rootProject.name = \"test\"")
    val file = root.resolve(relativePath)
    file.parent.createDirectories()
    file.writeText(code)
    return lint(compileForTest(file))
}
