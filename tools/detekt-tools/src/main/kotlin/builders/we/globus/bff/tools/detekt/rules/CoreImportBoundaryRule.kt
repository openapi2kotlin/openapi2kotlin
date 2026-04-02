package builders.we.globus.bff.tools.detekt.rules

import builders.we.globus.bff.tools.detekt.ext.isOpenApi2KotlinCoreFile
import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtImportDirective

class CoreImportBoundaryRule(
    config: Config,
) : Rule(config) {
    override val issue: Issue =
        Issue(
            id = "CoreImportBoundary",
            severity = Severity.Defect,
            description =
                "Openapi2kotlin application-core should only depend on internal code, " +
                    "Kotlin/JVM, coroutines, and logging.",
            debt = Debt.FIVE_MINS,
        )

    override fun visitImportDirective(importDirective: KtImportDirective) {
        super.visitImportDirective(importDirective)

        val file = importDirective.containingKtFile
        val importedFqName = importDirective.importedFqName?.asString() ?: return
        val isAllowedImport =
            file.isOpenApi2KotlinCoreFile() &&
                allowedImportPrefixes.any(importedFqName::startsWith)
        if (!file.isOpenApi2KotlinCoreFile() || isAllowedImport) {
            return
        }

        report(
            CodeSmell(
                issue = issue,
                entity = Entity.from(importDirective),
                message =
                    "Import '$importedFqName' is outside the allowed application-core boundary. " +
                        "Only internal imports, Kotlin/JVM, coroutines, and logging are allowed.",
            ),
        )
    }
}

private val allowedImportPrefixes =
    listOf(
        "dev.openapi2kotlin.",
        "java.",
        "kotlin.",
        "kotlin.jvm.",
        "kotlinx.coroutines.",
        "io.github.oshai.kotlinlogging.",
    )
