package builders.we.globus.bff.tools.detekt

import builders.we.globus.bff.tools.detekt.rules.CoreImportBoundaryRule
import builders.we.globus.bff.tools.detekt.rules.SuffixAwareTooManyFunctionsRule
import builders.we.globus.bff.tools.detekt.rules.TopLevelFunctionFileNameRule
import builders.we.globus.bff.tools.detekt.rules.UnusedTopLevelDeclarationRule
import builders.we.globus.bff.tools.detekt.rules.UnusedTopLevelFunctionRule
import builders.we.globus.bff.tools.detekt.rules.UsecaseDtoSuffixRule
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.RuleSet
import io.gitlab.arturbosch.detekt.api.RuleSetProvider

class HexagonalArchitectureRuleSetProvider : RuleSetProvider {
    override val ruleSetId: String = "hexagonalArchitecture"

    override fun instance(config: Config): RuleSet =
        RuleSet(
            id = ruleSetId,
            rules =
                listOf(
                    CoreImportBoundaryRule(config),
                    SuffixAwareTooManyFunctionsRule(config),
                    TopLevelFunctionFileNameRule(config),
                    UnusedTopLevelDeclarationRule(config),
                    UnusedTopLevelFunctionRule(config),
                    UsecaseDtoSuffixRule(config),
                ),
        )
}
