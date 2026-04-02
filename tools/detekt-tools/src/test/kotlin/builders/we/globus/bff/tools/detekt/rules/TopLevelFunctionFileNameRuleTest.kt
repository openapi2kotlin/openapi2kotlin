package builders.we.globus.bff.tools.detekt.rules

import io.gitlab.arturbosch.detekt.api.Config
import kotlin.test.Test
import kotlin.test.assertEquals

class TopLevelFunctionFileNameRuleTest {
    private val subject = TopLevelFunctionFileNameRule(Config.empty)

    @Test
    fun `reports mismatched file name for a single public top-level function with constants`() {
        val findings =
            subject.lintFile(
                fileName = "RegisterConnectAuth.kt",
                code =
                    """
                    package builders.we.globus.bff.tools.auth.connectauth

                    import io.ktor.server.application.Application

                    const val CONNECT_BEARER_TOKEN_HANDLER = "connect_bearer_token_handler"
                    private const val CONNECT_AUTH = "app.auth.connect"

                    fun Application.registerConnectAuth1() = Unit
                    """.trimIndent(),
            )

        assertEquals(1, findings.size)
    }

    @Test
    fun `ignores extension files handled by the dedicated extension rule`() {
        val findings =
            subject.lintFile(
                fileName = "ActionOfferDocumentsExt.kt",
                code =
                    """
                    package builders.we.globus.bff.modules.actionoffer.app.resource.ext

                    import builders.we.globus.bff.modules.actionoffer.app.resource.ActionOfferResource

                    fun ActionOfferResource.Houses.Number.Documents.toParams() = Unit
                    """.trimIndent(),
            )

        assertEquals(0, findings.size)
    }

    @Test
    fun `ignores dto extension files`() {
        val findings =
            subject.lintFile(
                fileName = "SearchEntityDtoExt.kt",
                code =
                    """
                    package builders.we.globus.bff.modules.productsearch.app.dto.ext

                    class SearchEntity

                    fun SearchEntity.toDto() = Unit
                    """.trimIndent(),
            )

        assertEquals(0, findings.size)
    }

    @Test
    fun `ignores files with top level type declarations using modifiers`() {
        val findings =
            subject.lintFile(
                fileName = "StoreStoryContentDto.kt",
                code =
                    """
                    package builders.we.globus.bff.modules.store.app.dto

                    open class StoreStoryContentDto

                    fun String.toDto() = StoreStoryContentDto()
                    """.trimIndent(),
            )

        assertEquals(0, findings.size)
    }

    @Test
    fun `ignores module application bootstrap files`() {
        val findings =
            subject.lintFile(
                fileName = "Application.kt",
                code =
                    """
                    package builders.we.globus.bff.modules.actionoffer.app

                    import io.ktor.server.application.Application

                    fun Application.ModuleActionOffer() = Unit
                    """.trimIndent(),
            )

        assertEquals(0, findings.size)
    }

    @Test
    fun `ignores test support files`() {
        val findings =
            subject.lintFile(
                fileName = "FoodPickupTestSupport.kt",
                code =
                    """
                    package builders.we.globus.bff.modules.foodpickup.app

                    import io.ktor.server.testing.ApplicationTestBuilder

                    fun ApplicationTestBuilder.configureFoodPickupTestModule() = Unit
                    """.trimIndent(),
            )

        assertEquals(0, findings.size)
    }
}
