package builders.we.globus.bff.tools.detekt.rules

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtNamedFunction

class SuffixAwareTooManyFunctionsRule(
    config: Config,
) : Rule(config) {
    override val issue: Issue =
        Issue(
            id = "SuffixAwareTooManyFunctions",
            severity = Severity.Maintainability,
            description =
                "Classes should stay reasonably small, while generated-style APIs and controllers " +
                    "may use a higher function threshold.",
            debt = Debt.FIVE_MINS,
        )

    private val defaultThreshold = valueOrDefault("defaultThreshold", DEFAULT_THRESHOLD)
    private val relaxedThreshold = valueOrDefault("relaxedThreshold", RELAXED_THRESHOLD)
    private val relaxedSuffixes =
        valueOrDefault(
            "relaxedSuffixes",
            listOf("Controller", "ApiImpl", "Api"),
        )

    override fun visitClass(klass: KtClass) {
        super.visitClass(klass)

        val className = klass.name ?: return
        val functionCount =
            klass.body
                ?.declarations
                ?.filterIsInstance<KtNamedFunction>()
                ?.count()
                ?: 0
        val threshold = thresholdFor(className)

        if (functionCount > threshold) {
            report(
                CodeSmell(
                    issue = issue,
                    entity = Entity.from(klass.nameIdentifier ?: klass),
                    message =
                        "Class '$className' declares $functionCount functions, which exceeds " +
                            "the allowed threshold of $threshold for this class name pattern.",
                ),
            )
        }
    }

    private fun thresholdFor(className: String): Int =
        if (relaxedSuffixes.any(className::endsWith)) {
            relaxedThreshold
        } else {
            defaultThreshold
        }

    private companion object {
        private const val DEFAULT_THRESHOLD = 11
        private const val RELAXED_THRESHOLD = 30
    }
}
