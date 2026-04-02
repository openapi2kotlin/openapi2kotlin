package builders.we.globus.bff.tools.detekt.rules

import io.gitlab.arturbosch.detekt.api.Config
import kotlin.test.Test
import kotlin.test.assertEquals

class SuffixAwareTooManyFunctionsRuleTest {
    private val subject = SuffixAwareTooManyFunctionsRule(Config.empty)

    @Test
    fun `reports regular class over default threshold`() {
        val code =
            """
            package demo

            class PlainService {
                fun one() = Unit
                fun two() = Unit
                fun three() = Unit
                fun four() = Unit
                fun five() = Unit
                fun six() = Unit
                fun seven() = Unit
                fun eight() = Unit
                fun nine() = Unit
                fun ten() = Unit
                fun eleven() = Unit
                fun twelve() = Unit
            }
            """.trimIndent()

        assertEquals(1, subject.lintFile("PlainService.kt", code).size)
    }

    @Test
    fun `does not report controller under relaxed threshold`() {
        val code =
            """
            package demo

            class PetController {
                fun one() = Unit
                fun two() = Unit
                fun three() = Unit
                fun four() = Unit
                fun five() = Unit
                fun six() = Unit
                fun seven() = Unit
                fun eight() = Unit
                fun nine() = Unit
                fun ten() = Unit
                fun eleven() = Unit
                fun twelve() = Unit
            }
            """.trimIndent()

        assertEquals(0, subject.lintFile("PetController.kt", code).size)
    }
}
