package builders.we.globus.bff.tools.detekt

import builders.we.globus.bff.tools.detekt.rules.CoreImportBoundaryRule
import builders.we.globus.bff.tools.detekt.rules.SuffixAwareTooManyFunctionsRule
import builders.we.globus.bff.tools.detekt.rules.TopLevelFunctionFileNameRule
import builders.we.globus.bff.tools.detekt.rules.UnusedTopLevelDeclarationRule
import builders.we.globus.bff.tools.detekt.rules.UnusedTopLevelFunctionRule
import builders.we.globus.bff.tools.detekt.rules.UsecaseDtoSuffixRule
import dev.detekt.api.RuleName
import dev.detekt.api.RuleSet
import dev.detekt.api.RuleSetId
import dev.detekt.api.RuleSetProvider

class HexagonalArchitectureRuleSetProvider : RuleSetProvider {
    override val ruleSetId: RuleSetId = RuleSetId("hexagonalArchitecture")

    override fun instance(): RuleSet =
        RuleSet(
            ruleSetId,
            rules =
                mapOf(
                    RuleName("CoreImportBoundary") to ::CoreImportBoundaryRule,
                    RuleName("SuffixAwareTooManyFunctions") to ::SuffixAwareTooManyFunctionsRule,
                    RuleName("TopLevelFunctionFileName") to ::TopLevelFunctionFileNameRule,
                    RuleName("UnusedTopLevelDeclaration") to ::UnusedTopLevelDeclarationRule,
                    RuleName("UnusedTopLevelFunction") to ::UnusedTopLevelFunctionRule,
                    RuleName("UsecaseDtoSuffix") to ::UsecaseDtoSuffixRule,
                ),
        )
}
