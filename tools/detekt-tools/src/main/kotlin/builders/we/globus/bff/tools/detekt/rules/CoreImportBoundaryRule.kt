package builders.we.globus.bff.tools.detekt.rules

import builders.we.globus.bff.tools.detekt.ext.isOpenApi2KotlinCoreFile
import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import org.jetbrains.kotlin.psi.KtImportDirective

class CoreImportBoundaryRule(
    config: Config,
) : Rule(
        config,
        "Openapi2kotlin application-core should only depend on internal code, Kotlin/JVM, coroutines, and logging.",
    ) {
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
            Finding(
                Entity.from(importDirective),
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
