package builders.we.globus.bff.tools.detekt.rules

import io.gitlab.arturbosch.detekt.api.Config
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

class UnusedTopLevelFunctionRuleTest {
    private val subject = UnusedTopLevelFunctionRule(Config.empty)

    @Test
    fun `reports unused top level function`() {
        val root = Files.createTempDirectory("unused-top-level-function-rule")
        val findings =
            subject.lintRepoFile(
                root = root,
                relativePath =
                    "tools/logging-tools/src/main/kotlin/" +
                        "builders/we/globus/bff/tools/logging/CurrentLoggingContext.kt",
                code =
                    """
                    package builders.we.globus.bff.tools.logging

                    fun currentLoggingContext() = emptyMap<String, String>()
                    """.trimIndent(),
            )

        assertEquals(1, findings.size)
        assertEquals(
            "Function 'currentLoggingContext' is not referenced outside " +
                "'CurrentLoggingContext.kt'. Remove it or reference it.",
            findings.single().message,
        )
    }

    @Test
    fun `reports unused member extension function`() {
        val root = Files.createTempDirectory("unused-member-extension-function-rule")
        val findings =
            subject.lintRepoFile(
                root = root,
                relativePath =
                    "modules/product-catalog/product-catalog-usecase/src/main/kotlin/" +
                        "builders/we/globus/bff/modules/productcatalog/usecase/internal/" +
                        "GetProductCategoriesService.kt",
                code =
                    """
                    package builders.we.globus.bff.modules.productcatalog.usecase.internal

                    internal class GetProductCategoriesService {
                        fun String.unusedFun(): String = String.Companion::class.java.toString()
                    }
                    """.trimIndent(),
            )

        assertEquals(1, findings.size)
        assertEquals(
            "Function 'unusedFun' is not referenced outside " +
                "'GetProductCategoriesService.kt'. Remove it or reference it.",
            findings.single().message,
        )
    }

    @Test
    fun `ignores detekt rule tests when counting usages`() {
        val root = Files.createTempDirectory("unused-function-ignores-rule-tests")
        root.resolve(
            "tools/detekt-tools/src/test/kotlin/" +
                "builders/we/globus/bff/tools/detekt/rules/UnusedTopLevelFunctionRuleTest.kt",
        ).apply {
            parent.toFile().mkdirs()
            toFile().writeText(
                """
                package builders.we.globus.bff.tools.detekt.rules

                class UnusedTopLevelFunctionRuleTest {
                    fun message() = "unusedFun"
                }
                """.trimIndent(),
            )
        }

        val findings =
            subject.lintRepoFile(
                root = root,
                relativePath =
                    "modules/product-catalog/product-catalog-usecase/src/main/kotlin/" +
                        "builders/we/globus/bff/modules/productcatalog/usecase/internal/" +
                        "GetProductCategoriesService.kt",
                code =
                    """
                    package builders.we.globus.bff.modules.productcatalog.usecase.internal

                    internal class GetProductCategoriesService {
                        fun String.unusedFun(): String = String.Companion::class.java.toString()
                    }
                    """.trimIndent(),
            )

        assertEquals(1, findings.size)
    }

    @Test
    fun `accepts used extension function`() {
        val root = Files.createTempDirectory("used-extension-function-rule")
        root.resolve(
            "modules/coupon/coupon-app/src/main/kotlin/" +
                "builders/we/globus/bff/modules/coupon/app/routing/CouponRoute.kt",
        ).apply {
            parent.toFile().mkdirs()
            toFile().writeText(
                """
                package builders.we.globus.bff.modules.coupon.app.routing

                import builders.we.globus.bff.modules.coupon.app.dto.ext.toDto

                fun CouponRoute() = "coupon".toDto()
                """.trimIndent(),
            )
        }

        val findings =
            subject.lintRepoFile(
                root = root,
                relativePath =
                    "modules/coupon/coupon-app/src/main/kotlin/" +
                        "builders/we/globus/bff/modules/coupon/app/dto/ext/CouponExt.kt",
                code =
                    """
                    package builders.we.globus.bff.modules.coupon.app.dto.ext

                    fun String.toDto() = this
                    """.trimIndent(),
            )

        assertEquals(0, findings.size)
    }

    @Test
    fun `accepts module function referenced from yaml`() {
        val root = Files.createTempDirectory("module-function-rule")
        root.resolve("app/src/main/resources/application.yaml").apply {
            parent.toFile().mkdirs()
            toFile().writeText(
                """
                ktor:
                  application:
                    modules:
                      - builders.we.globus.bff.modules.coupon.app.ApplicationKt.moduleCoupon
                """.trimIndent(),
            )
        }

        val findings =
            subject.lintRepoFile(
                root = root,
                relativePath =
                    "modules/coupon/coupon-app/src/main/kotlin/" +
                        "builders/we/globus/bff/modules/coupon/app/Application.kt",
                code =
                    """
                    package builders.we.globus.bff.modules.coupon.app

                    import io.ktor.server.application.Application

                    fun Application.moduleCoupon() = Unit
                    """.trimIndent(),
            )

        assertEquals(0, findings.size)
    }
}
