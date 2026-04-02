package builders.we.globus.bff.tools.detekt.rules

import io.gitlab.arturbosch.detekt.api.Config
import kotlin.test.Test
import kotlin.test.assertEquals

class CoreImportBoundaryRuleTest {
    private val subject = CoreImportBoundaryRule(Config.empty)

    @Test
    fun `allows internal, jvm, coroutines, and logging imports in application core`() {
        val findings =
            subject.lintFile(
                fileName = "PrepareApiContext.kt",
                code =
                    """
                    package dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.api

                    import dev.openapi2kotlin.application.usecase.openapi2kotlin.OpenApi2KotlinUseCase
                    import io.github.oshai.kotlinlogging.KotlinLogging
                    import java.nio.file.Path
                    import kotlin.jvm.JvmInline
                    import kotlinx.coroutines.async

                    fun prepareApiContext(path: Path) {
                        async {}
                    }
                    """.trimIndent(),
            )

        assertEquals(0, findings.size)
    }

    @Test
    fun `reports framework import in application core`() {
        val findings =
            subject.lintFile(
                fileName = "PrepareApiContext.kt",
                code =
                    """
                    package dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.api

                    import com.fasterxml.jackson.databind.ObjectMapper

                    fun prepareApiContext(mapper: ObjectMapper) = mapper.toString()
                    """.trimIndent(),
            )

        assertEquals(1, findings.size)
    }
}
