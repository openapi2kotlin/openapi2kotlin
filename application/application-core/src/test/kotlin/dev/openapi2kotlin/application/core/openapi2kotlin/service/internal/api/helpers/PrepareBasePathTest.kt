package dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.api.helpers

import dev.openapi2kotlin.application.core.openapi2kotlin.model.raw.RawServerDO
import dev.openapi2kotlin.application.usecase.openapi2kotlin.OpenApi2KotlinUseCase
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals

class PrepareBasePathTest {
    @Test
    fun `prepareBasePath prefers matching basePathVar server variable`() {
        val result = prepareBasePath(
            rawServers = listOf(
                RawServerDO(
                    url = "/v2/",
                    vars = null,
                ),
                RawServerDO(
                    url = "/{basePath}",
                    vars = listOf(
                        RawServerDO.Var(
                            name = "basePath",
                            defaultValue = "v5/",
                        ),
                    ),
                ),
            ),
            config = config(basePathVar = "basePath"),
        )

        assertEquals("/v5", result)
    }

    @Test
    fun `prepareBasePath falls back to first relative url when basePathVar is blank`() {
        val result = prepareBasePath(
            rawServers = listOf(
                RawServerDO(
                    url = "https://serverRoot.example.com",
                    vars = null,
                ),
                RawServerDO(
                    url = "/v5/",
                    vars = null,
                ),
            ),
            config = config(basePathVar = " "),
        )

        assertEquals("/v5", result)
    }

    @Test
    fun `prepareBasePath stays empty when basePathVar is blank and no relative url exists`() {
        val result = prepareBasePath(
            rawServers = listOf(
                RawServerDO(
                    url = "https://serverRoot.example.com/api",
                    vars = null,
                ),
            ),
            config = config(basePathVar = ""),
        )

        assertEquals("", result)
    }

    @Test
    fun `prepareBasePath stays empty when configured basePathVar has no matching server variable`() {
        val result = prepareBasePath(
            rawServers = listOf(
                RawServerDO(
                    url = "/v5/",
                    vars = null,
                ),
                RawServerDO(
                    url = "/{otherPath}",
                    vars = listOf(
                        RawServerDO.Var(
                            name = "otherPath",
                            defaultValue = "v7/",
                        ),
                    ),
                ),
            ),
            config = config(basePathVar = "basePath"),
        )

        assertEquals("", result)
    }

    private fun config(basePathVar: String): OpenApi2KotlinUseCase.Config =
        OpenApi2KotlinUseCase.Config(
            inputSpecPath = Paths.get("openapi.yaml"),
            outputDirPath = Paths.get("build/generated"),
            model = OpenApi2KotlinUseCase.ModelConfig(
                packageName = "org.tmforum.tmf620.model",
                serialization = OpenApi2KotlinUseCase.ModelConfig.Serialization.KOTLINX,
                validation = null,
                double2BigDecimal = false,
                float2BigDecimal = false,
                integer2Long = true,
            ),
            api = OpenApi2KotlinUseCase.ApiConfig.ClientKtor(
                packageName = "org.tmforum.tmf620.client",
                basePathVar = basePathVar,
            ),
        )
}
