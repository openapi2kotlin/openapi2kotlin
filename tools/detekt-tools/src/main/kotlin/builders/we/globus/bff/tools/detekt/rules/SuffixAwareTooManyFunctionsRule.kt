package builders.we.globus.bff.tools.detekt.rules

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtNamedFunction

class SuffixAwareTooManyFunctionsRule(
    config: Config,
) : Rule(
        config,
        "Classes should stay reasonably small, while generated-style APIs and controllers " +
            "may use a higher function threshold.",
    ) {
    private val defaultThreshold = config.valueOrDefault("defaultThreshold", DEFAULT_THRESHOLD)
    private val relaxedThreshold = config.valueOrDefault("relaxedThreshold", RELAXED_THRESHOLD)
    private val relaxedSuffixes =
        config.valueOrDefault(
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
                Finding(
                    Entity.from(klass.nameIdentifier ?: klass),
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
