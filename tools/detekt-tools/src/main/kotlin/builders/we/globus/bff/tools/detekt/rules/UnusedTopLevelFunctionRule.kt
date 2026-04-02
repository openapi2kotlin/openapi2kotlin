package builders.we.globus.bff.tools.detekt.rules

import builders.we.globus.bff.tools.detekt.ext.cachedRepositoryTextIndex
import builders.we.globus.bff.tools.detekt.ext.repoRoot
import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import java.nio.file.Path

class UnusedTopLevelFunctionRule(
    config: Config,
) : Rule(config) {
    override val issue: Issue =
        Issue(
            id = "UnusedTopLevelFunction",
            severity = Severity.Defect,
            description = "Functions with bodies should be referenced somewhere in the repository or removed.",
            debt = Debt.TWENTY_MINS,
        )

    override fun visitKtFile(file: KtFile) {
        super.visitKtFile(file)

        val repoRoot = file.repoRoot() ?: return
        val repositoryIndex =
            cachedRepositoryTextIndex(
                root = repoRoot,
                pattern = FUNCTION_IDENTIFIER_PATTERN,
                supportedExtensions = SUPPORTED_EXTENSIONS,
                includeTestSources = true,
            )

        file.collectDescendantsOfType<KtNamedFunction>()
            .filterNot(::shouldIgnore)
            .forEach { function ->
                val functionName = function.name ?: return@forEach
                if (repositoryIndex.occurrences(functionName) <= 1) {
                    val displayFileName = Path.of(file.name).fileName.toString()
                    report(
                        CodeSmell(
                            issue = issue,
                            entity = Entity.from(function),
                            message =
                                "Function '$functionName' is not referenced outside " +
                                    "'$displayFileName'. Remove it or reference it.",
                        ),
                    )
                }
            }
    }
}

private fun shouldIgnore(function: KtNamedFunction): Boolean {
    val name = function.name ?: return true
    val file = function.containingKtFile
    val isPrivate = (function as? KtModifierListOwner)?.modifierList?.text?.contains("private") == true
    val isOverride = function.hasModifier(KtTokens.OVERRIDE_KEYWORD)
    val isLocal = function.parent is KtBlockExpression
    val hasBody = function.bodyExpression != null

    return !hasBody ||
        isLocal ||
        isPrivate ||
        isOverride ||
        name == "main" ||
        name.endsWith("UseCaseProvider") ||
        name.startsWith("component") ||
        file.name.endsWith("Support.kt") ||
        file.name.endsWith("Test.kt")
}

private val FUNCTION_IDENTIFIER_PATTERN = Regex("""\b[a-zA-Z][A-Za-z0-9_]*\b""")
private val SUPPORTED_EXTENSIONS = setOf("kt", "kts", "yml", "yaml", "properties", "http")
