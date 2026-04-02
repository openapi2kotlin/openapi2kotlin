package builders.we.globus.bff.tools.detekt.rules

import io.gitlab.arturbosch.detekt.api.Config
import kotlin.test.Test
import kotlin.test.assertEquals

class UsecaseDtoSuffixRuleTest {
    private val subject = UsecaseDtoSuffixRule(Config.empty)

    @Test
    fun `reports missing do suffix in openapi2kotlin core domain`() {
        val findings =
            subject.lintFile(
                fileName = "Coupon.kt",
                code =
                    """
                    package dev.openapi2kotlin.application.core.openapi2kotlin.domain.model

                    data class Coupon(
                        val id: Long,
                    )
                    """.trimIndent(),
            )

        assertEquals(1, findings.size)
    }

    @Test
    fun `allows do suffix in openapi2kotlin core domain`() {
        val findings =
            subject.lintFile(
                fileName = "CouponDO.kt",
                code =
                    """
                    package dev.openapi2kotlin.application.core.openapi2kotlin.domain.model

                    data class CouponDO(
                        val id: Long,
                    )
                    """.trimIndent(),
            )

        assertEquals(0, findings.size)
    }
}
