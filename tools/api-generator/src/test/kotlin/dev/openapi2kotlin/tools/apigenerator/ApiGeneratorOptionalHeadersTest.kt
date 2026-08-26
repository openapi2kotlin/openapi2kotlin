package dev.openapi2kotlin.tools.apigenerator

import dev.openapi2kotlin.application.core.openapi2kotlin.domain.api.ApiContextDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.api.ApiDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.api.ApiEndpointDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.api.ApiParamDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.model.TrivialTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.raw.RawPathDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.raw.RawSchemaDO
import dev.openapi2kotlin.application.core.openapi2kotlin.port.GenerateApiPort
import kotlin.io.path.createTempDirectory
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertContains

class ApiGeneratorOptionalHeadersTest {
    @Test
    fun `makes even required client headers nullable with a null default`() {
        val outputDir = createTempDirectory("openapi2kotlin-optional-headers-test")
        val requiredHeader =
            RawPathDO.ParamDO(
                name = "X-Request-Id",
                location = RawPathDO.ParamLocationDO.HEADER,
                required = true,
                type =
                    RawSchemaDO.RawPrimitiveTypeDO(
                        type = RawSchemaDO.RawPrimitiveTypeDO.Type.STRING,
                        nullable = false,
                    ),
            )
        val operation =
            RawPathDO.OperationDO(
                tags = listOf("test"),
                operationId = "retrieveTest",
                httpMethod = RawPathDO.HttpMethodDO.GET,
                path = "/test",
                summary = null,
                description = null,
                parameters = listOf(requiredHeader),
                requestBody = null,
                responses = null,
            )
        val endpoint =
            ApiEndpointDO(
                rawOperation = operation,
                generatedName = "retrieveTest",
                params =
                    listOf(
                        ApiParamDO(
                            rawParam = requiredHeader,
                            generatedName = "xRequestId",
                            type = TrivialTypeDO(TrivialTypeDO.Kind.STRING, nullable = false),
                        ),
                    ),
                requestBody = null,
                successResponse = null,
            )
        val api =
            ApiDO(
                rawPath = RawPathDO(tags = listOf("test"), operations = listOf(operation)),
                generatedName = "TestApi",
                endpoints = listOf(endpoint),
            )

        ApiGenerator(optionalHeaders = true).generateApi(
            GenerateApiPort.Command(
                apiContext = ApiContextDO(apis = listOf(api), basePath = "/"),
                packageName = "example.client",
                modelPackageName = "example.model",
                outputDirPath = outputDir,
                models = emptyList(),
            ),
        )

        val generated = outputDir.resolve("example/client/TestApi.kt").readText()
        assertContains(generated, "xRequestId: String? = null")
    }
}
