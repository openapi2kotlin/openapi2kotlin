package builders.we.globus.bff.tools.detekt.rules

import io.gitlab.arturbosch.detekt.api.Config
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

class UnusedTopLevelDeclarationRuleTest {
    private val subject = UnusedTopLevelDeclarationRule(Config.empty)

    @Test
    fun `reports unused top level declaration`() {
        val root = Files.createTempDirectory("unused-top-level-rule")
        val findings =
            subject.lintRepoFile(
                root = root,
                relativePath =
                    "modules/action-offer/action-offer-app/src/main/kotlin/" +
                        "builders/we/globus/bff/modules/actionoffer/app/dto/" +
                        "ActionOfferLastModifiedDto.kt",
                code =
                    """
                    package builders.we.globus.bff.modules.actionoffer.app.dto

                    data class ActionOfferLastModifiedDto(
                        val lastModified: String,
                    )
                    """.trimIndent(),
            )

        assertEquals(1, findings.size)
        assertEquals(
            "Top-level declaration 'ActionOfferLastModifiedDto' is not referenced outside " +
                "'ActionOfferLastModifiedDto.kt'. Remove it or reference it.",
            findings.single().message,
        )
    }

    @Test
    fun `accepts declaration used from another file`() {
        val root = Files.createTempDirectory("used-top-level-rule")
        root.resolve(
            "modules/action-offer/action-offer-app/src/main/kotlin/" +
                "builders/we/globus/bff/modules/actionoffer/app/routing/SearchProductsRoute.kt",
        ).apply {
            parent.toFile().mkdirs()
            toFile().writeText(
                """
                package builders.we.globus.bff.modules.actionoffer.app.routing

                import builders.we.globus.bff.modules.actionoffer.app.dto.ActionOfferLastModifiedDto

                fun SearchProductsRoute() = ActionOfferLastModifiedDto(lastModified = "now")
                """.trimIndent(),
            )
        }

        val findings =
            subject.lintRepoFile(
                root = root,
                relativePath =
                    "modules/action-offer/action-offer-app/src/main/kotlin/" +
                        "builders/we/globus/bff/modules/actionoffer/app/dto/" +
                        "ActionOfferLastModifiedDto.kt",
                code =
                    """
                    package builders.we.globus.bff.modules.actionoffer.app.dto

                    data class ActionOfferLastModifiedDto(
                        val lastModified: String,
                    )
                    """.trimIndent(),
            )

        assertEquals(0, findings.size)
    }

    @Test
    fun `ignores excluded declaration suffix`() {
        val root = Files.createTempDirectory("excluded-top-level-rule")
        val findings =
            subject.lintRepoFile(
                root = root,
                relativePath =
                    "networks/action-offer-network/src/main/kotlin/" +
                        "builders/we/globus/bff/networks/actionoffer/api/" +
                        "ActionOfferApi.kt",
                code =
                    """
                    package builders.we.globus.bff.networks.actionoffer.api

                    interface ActionOfferApi
                    """.trimIndent(),
            )

        assertEquals(0, findings.size)
    }
}
