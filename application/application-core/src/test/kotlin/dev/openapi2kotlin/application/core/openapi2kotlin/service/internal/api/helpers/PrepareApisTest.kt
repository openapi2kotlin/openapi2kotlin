package dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.api.helpers

import dev.openapi2kotlin.application.core.openapi2kotlin.domain.raw.RawPathDO
import dev.openapi2kotlin.application.usecase.openapi2kotlin.OpenApi2KotlinUseCase
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals

class PrepareApisTest {
    @Test
    fun `prepareApis keeps path based API grouping by default`() {
        val apis =
            prepareApis(
                rawPaths = rawPaths(),
                models = emptyList(),
                config = config(apiNameFromTags = false),
            )

        assertEquals(listOf("ApiApi"), apis.map { it.generatedName })
    }

    @Test
    fun `prepareApis can group APIs by tags when enabled`() {
        val apis =
            prepareApis(
                rawPaths = rawPaths(),
                models = emptyList(),
                config = config(apiNameFromTags = true),
            )

        assertEquals(listOf("CouponsApi", "ProductCatalogApi"), apis.map { it.generatedName })
    }

    private fun config(apiNameFromTags: Boolean): OpenApi2KotlinUseCase.Config =
        OpenApi2KotlinUseCase.Config(
            inputSpecPath = Paths.get("openapi.yaml"),
            outputDirPath = Paths.get("build/generated"),
            model =
                OpenApi2KotlinUseCase.ModelConfig(
                    packageName = "dev.openapi2kotlin.model",
                    serialization = OpenApi2KotlinUseCase.ModelConfig.Serialization.KOTLINX,
                    validation = null,
                    double2BigDecimal = false,
                    float2BigDecimal = false,
                    integer2Long = true,
                ),
            api =
                OpenApi2KotlinUseCase.ApiConfig.ClientKtor(
                    packageName = "dev.openapi2kotlin.client",
                    basePathVar = "basePath",
                    apiNameFromTags = apiNameFromTags,
                ),
        )

    private fun rawPaths(): List<RawPathDO> =
        listOf(
            RawPathDO(
                tags = listOf("api", "Coupons", "ProductCatalog"),
                operations =
                    listOf(
                        operation(
                            operationId = "translateCoupons",
                            path = "/api/v1/coupons/translate",
                            tags = listOf("Coupons"),
                        ),
                        operation(
                            operationId = "translateProducts",
                            path = "/api/v1/productCatalog/products/translate",
                            tags = listOf("ProductCatalog"),
                        ),
                    ),
            ),
        )

    private fun operation(
        operationId: String,
        path: String,
        tags: List<String>,
    ) = RawPathDO.OperationDO(
        tags = tags,
        operationId = operationId,
        httpMethod = RawPathDO.HttpMethodDO.POST,
        path = path,
        summary = null,
        description = null,
        parameters = emptyList(),
        requestBody = null,
        responses = emptyList(),
    )
}
