package builders.we.globus.bff.tools.detekt.rules

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtFile

class TopLevelFunctionFileNameRule(
    config: Config,
) : Rule(config) {
    override val issue: Issue =
        Issue(
            id = "TopLevelFunctionFileName",
            severity = Severity.Defect,
            description = "Single public top-level function files should match the function name.",
            debt = Debt.FIVE_MINS,
        )

    override fun visitKtFile(file: KtFile) {
        super.visitKtFile(file)

        val actualFileName = file.name.substringAfterLast('/').substringAfterLast('\\')
        val normalizedPath = file.virtualFilePath.replace('\\', '/')
        val packageName = file.packageFqName.asString()

        val isConventionExtensionFile =
            normalizedPath.contains("/ext/") || packageName.contains(".ext")
        val isModuleApplicationFile =
            actualFileName == "Application.kt" &&
                (
                    (
                        normalizedPath.contains("/modules/") ||
                            packageName.contains(".modules.")
                    ) &&
                        packageName.contains(".app") ||
                        file.text.containsKtorApplicationModule()
                )

        val shouldSkipFile =
            listOf(
                actualFileName.endsWith("UseCase.kt"),
                actualFileName.endsWith("TestSupport.kt"),
                isConventionExtensionFile,
                isModuleApplicationFile,
                file.text.containsTopLevelTypeDeclaration(),
            ).any { it }

        if (shouldSkipFile) {
            return
        }

        val topLevelFunctionNames = file.text.topLevelFunctionNames()
        val functionName = topLevelFunctionNames.singleOrNull() ?: return
        val expectedFileName = functionName.replaceFirstChar(Char::titlecase) + ".kt"
        val matchesFileName = actualFileName.substringBeforeLast('.').lowercase() == functionName.lowercase()

        if (!matchesFileName) {
            report(
                CodeSmell(
                    issue = issue,
                    entity = Entity.from(file),
                    message =
                        "File '$actualFileName' should be named '$expectedFileName' to match " +
                            "its single public top-level function '$functionName'.",
                ),
            )
        }
    }

    private fun String.containsTopLevelTypeDeclaration(): Boolean {
        return TOP_LEVEL_TYPE_DECLARATION.containsMatchIn(this)
    }

    private fun String.topLevelFunctionNames(): List<String> =
        TOP_LEVEL_FUNCTION.findAll(this).map { it.groupValues[1] }.toList()

    private fun String.containsKtorApplicationModule(): Boolean {
        return KTOR_APPLICATION_MODULE.containsMatchIn(this)
    }

    private companion object {
        private val TOP_LEVEL_FUNCTION =
            Regex(
                pattern = """(?m)^(?:public\s+)?fun\s+(?:[A-Za-z0-9_.<>?, ]+\.)?([A-Za-z][A-Za-z0-9_]*)\s*\(""",
            )
        private val TOP_LEVEL_TYPE_DECLARATION =
            Regex(
                pattern = """(?m)^(?:[A-Za-z]+\s+)*(?:class|interface|object|typealias)\s+""",
            )
        private val KTOR_APPLICATION_MODULE =
            Regex(
                pattern = """(?m)^fun\s+Application\.module\s*\(""",
            )
    }
}
