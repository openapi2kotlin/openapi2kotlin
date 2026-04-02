package builders.we.globus.bff.tools.detekt.rules

import builders.we.globus.bff.tools.detekt.ext.absolutePath
import builders.we.globus.bff.tools.detekt.ext.cachedRepositoryTextIndex
import builders.we.globus.bff.tools.detekt.ext.repoRoot
import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtTypeAlias
import java.nio.file.Files
import java.nio.file.Path

class UnusedTopLevelDeclarationRule(
    config: Config,
) : Rule(config) {
    override val issue: Issue =
        Issue(
            id = "UnusedTopLevelDeclaration",
            severity = Severity.Defect,
            description =
                "Top-level declarations should be referenced somewhere else in the repository or removed.",
            debt = Debt.TWENTY_MINS,
        )

    override fun visitKtFile(file: KtFile) {
        super.visitKtFile(file)

        val repoRoot = file.repoRoot() ?: return
        val filePath = file.absolutePath() ?: return
        val repositoryIndex =
            cachedRepositoryTextIndex(
                root = repoRoot,
                pattern = IDENTIFIER_PATTERN,
                supportedExtensions = SUPPORTED_EXTENSIONS,
                includeTestSources = true,
            )

        file.topLevelDeclarations()
            .filterNot(::shouldIgnore)
            .forEach { declaration ->
                val declarationName = declaration.name ?: return@forEach
                val occurrencesOutsideFile =
                    repositoryIndex.occurrences(declarationName) -
                        identifierOccurrences(filePath, declarationName)

                if (occurrencesOutsideFile <= 0) {
                    val displayFileName = Path.of(file.name).fileName.toString()
                    report(
                        CodeSmell(
                            issue = issue,
                            entity = Entity.from(declaration),
                            message =
                                "Top-level declaration '$declarationName' is not referenced " +
                                    "outside '$displayFileName'. Remove it or reference it.",
                        ),
                    )
                }
            }
    }
}

private fun KtFile.topLevelDeclarations(): List<KtNamedDeclaration> =
    declarations.filterIsInstance<KtClass>() +
        declarations.filterIsInstance<KtObjectDeclaration>() +
        declarations.filterIsInstance<KtTypeAlias>()

private fun shouldIgnore(declaration: KtNamedDeclaration): Boolean {
    val name = declaration.name ?: return true
    val file = declaration.containingKtFile
    val isNonPublic = (declaration as? KtModifierListOwner)?.isNonPublic() == true
    val hasFrameworkManagedAnnotation = declaration.hasFrameworkManagedAnnotation()

    return isNonPublic ||
        hasFrameworkManagedAnnotation ||
        name in setOf("Application", "Companion") ||
        name.hasIgnoredSuffix() ||
        file.name.endsWith("Test.kt")
}

private fun String.hasIgnoredSuffix(): Boolean = ignoredSuffixes.any(::endsWith)

private fun KtModifierListOwner.isNonPublic(): Boolean =
    modifierList?.text?.let { "private" in it || "internal" in it } == true

private fun KtNamedDeclaration.hasFrameworkManagedAnnotation(): Boolean =
    annotationEntries.any { annotation ->
        annotation.shortName?.asString() in
            setOf(
                "Configuration",
                "SpringBootApplication",
                "ConfigurationProperties",
                "Component",
                "Service",
                "Repository",
                "Controller",
                "RestController",
            )
    }

private fun identifierOccurrences(
    file: Path,
    identifier: String,
): Int = identifierRegex(identifier).findAll(Files.readString(file)).count()

private fun identifierRegex(identifier: String): Regex = Regex("""\b${Regex.escape(identifier)}\b""")

private val IDENTIFIER_PATTERN = Regex("""\b[A-Z][A-Za-z0-9_]*\b""")

private val ignoredSuffixes =
    setOf(
        "Serializer",
        "Resource",
        "UseCase",
        "Port",
        "Service",
        "Adapter",
        "Api",
        "Config",
        "Module",
        "Rule",
        "RuleSetProvider",
        "TestSupport",
    )

private val SUPPORTED_EXTENSIONS =
    setOf(
        "kt",
        "kts",
        "java",
        "properties",
        "xml",
        "md",
    )
