package builders.we.globus.bff.tools.detekt.rules

import builders.we.globus.bff.tools.detekt.ext.isOpenApi2KotlinCoreDomainFile
import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtTypeAlias

class UsecaseDtoSuffixRule(
    config: Config,
) : Rule(config) {
    override val issue: Issue =
        Issue(
            id = "UsecaseDtoSuffix",
            severity = Severity.Defect,
            description = "Openapi2kotlin core domain types must use the DO suffix.",
            debt = Debt.FIVE_MINS,
        )

    override fun visitClass(klass: KtClass) {
        super.visitClass(klass)
        reportIfInvalidName(
            isCoreDomainFile = klass.containingKtFile.isOpenApi2KotlinCoreDomainFile(),
            isTopLevelDeclaration = klass.parent is KtFile,
            typeName = klass.name,
            entity = Entity.from(klass),
        )
    }

    override fun visitObjectDeclaration(declaration: KtObjectDeclaration) {
        super.visitObjectDeclaration(declaration)
        reportIfInvalidName(
            isCoreDomainFile = declaration.containingKtFile.isOpenApi2KotlinCoreDomainFile(),
            isTopLevelDeclaration = declaration.parent is KtFile,
            typeName = declaration.name,
            entity = Entity.from(declaration),
        )
    }

    override fun visitTypeAlias(typeAlias: KtTypeAlias) {
        super.visitTypeAlias(typeAlias)
        reportIfInvalidName(
            isCoreDomainFile = typeAlias.containingKtFile.isOpenApi2KotlinCoreDomainFile(),
            isTopLevelDeclaration = typeAlias.parent is KtFile,
            typeName = typeAlias.name,
            entity = Entity.from(typeAlias),
        )
    }

    private fun reportIfInvalidName(
        isCoreDomainFile: Boolean,
        isTopLevelDeclaration: Boolean,
        typeName: String?,
        entity: Entity,
    ) {
        if (!isCoreDomainFile || !isTopLevelDeclaration) {
            return
        }

        if (typeName.isNullOrBlank() || typeName.endsWith("DO")) {
            return
        }

        report(
            CodeSmell(
                issue = issue,
                entity = entity,
                message = "Openapi2kotlin core domain type '$typeName' must use the DO suffix.",
            ),
        )
    }
}
