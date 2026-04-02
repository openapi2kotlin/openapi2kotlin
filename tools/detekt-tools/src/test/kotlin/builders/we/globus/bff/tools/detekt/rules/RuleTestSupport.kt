package builders.we.globus.bff.tools.detekt.rules

import io.github.detekt.test.utils.compileContentForTest
import io.gitlab.arturbosch.detekt.api.BaseRule
import io.gitlab.arturbosch.detekt.api.Finding
import io.gitlab.arturbosch.detekt.test.lint
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

internal fun BaseRule.lintFile(
    fileName: String,
    code: String,
): List<Finding> = lint(compileContentForTest(code, fileName))

internal fun BaseRule.lintRepoFile(
    root: Path,
    relativePath: String,
    code: String,
): List<Finding> {
    root.resolve("settings.gradle.kts").writeText("rootProject.name = \"test\"")
    val file = root.resolve(relativePath)
    file.parent.createDirectories()
    file.writeText(code)
    return lint(compileContentForTest(code, file.toString()))
}
