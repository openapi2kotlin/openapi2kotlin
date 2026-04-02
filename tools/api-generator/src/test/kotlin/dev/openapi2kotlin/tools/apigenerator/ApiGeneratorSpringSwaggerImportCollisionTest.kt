package dev.openapi2kotlin.tools.apigenerator

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.api.ApiAnnotationDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.api.ApiContextDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.api.ApiDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.api.ApiEndpointDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.api.ApiSuccessResponseDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.model.ModelDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.model.ModelShapeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.model.RefTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.raw.RawPathDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.raw.RawSchemaDO
import dev.openapi2kotlin.application.core.openapi2kotlin.port.GenerateApiPort
import dev.openapi2kotlin.tools.generatortools.TypeNameContext
import dev.openapi2kotlin.tools.generatortools.toTypeName
import kotlin.io.path.createTempDirectory
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertContains

class ApiGeneratorSpringSwaggerImportCollisionTest {
    @Test
    fun `aliases annotation import when model simple name collides`() {
        val outputDir = createTempDirectory("openapi2kotlin-spring-alias-test")
        val modelPackage = "demo.server.spring.generated.model"
        val apiPackage = "demo.server.spring.generated.server"
        val apiResponseModel = createApiResponseModel(modelPackage)
        val operation = createUploadFileOperation()
        val endpoint = createUploadFileEndpoint(operation, modelPackage)
        val api = createPetApi(operation, endpoint)

        ApiGenerator(TestSpringPolicy).generateApi(
            GenerateApiPort.Command(
                apiContext =
                    ApiContextDO(
                        apis = listOf(api),
                        basePath = "/api/v3",
                    ),
                packageName = apiPackage,
                modelPackageName = modelPackage,
                outputDirPath = outputDir,
                models = listOf(apiResponseModel),
            ),
        )

        val generated = outputDir.resolve("demo/server/spring/generated/server/PetApi.kt").readText()

        assertContains(generated, "import demo.server.spring.generated.model.ApiResponse")
        assertContains(
            generated,
            "import io.swagger.v3.oas.annotations.responses.ApiResponse as ResponsesApiResponseAnnotation",
        )
        assertContains(generated, "responses = [ResponsesApiResponseAnnotation(")
        assertContains(generated, "ResponseEntity<ApiResponse>")
        assertContains(generated, "implementation = ApiResponse::class")
    }

    private fun createApiResponseModel(modelPackage: String): ModelDO =
        ModelDO(
            rawSchema =
                RawSchemaDO(
                    originalName = "ApiResponse",
                    allOfParents = emptyList(),
                    oneOfChildren = emptyList(),
                    discriminatorMapping = emptyMap(),
                    isDiscriminatorSelfMapped = false,
                ),
            packageName = modelPackage,
            generatedName = "ApiResponse",
            modelShape =
                ModelShapeDO.EmptyClass(
                    extend = null,
                    implements = emptyList(),
                ),
        )

    private fun createUploadFileOperation(): RawPathDO.OperationDO =
        RawPathDO.OperationDO(
            operationId = "uploadFile",
            httpMethod = RawPathDO.HttpMethodDO.POST,
            path = "/pet/{petId}/uploadImage",
            summary = "Uploads an image.",
            description = "Upload image of the pet.",
            parameters = emptyList(),
            requestBody = null,
            responses =
                listOf(
                    RawPathDO.ResponseDO(
                        statusCode = 200,
                        type = RawSchemaDO.RawRefTypeDO("ApiResponse", false),
                    ),
                ),
        )

    private fun createUploadFileEndpoint(
        operation: RawPathDO.OperationDO,
        modelPackage: String,
    ): ApiEndpointDO =
        ApiEndpointDO(
            rawOperation = operation,
            generatedName = "uploadFile",
            params = emptyList(),
            requestBody = null,
            successResponse =
                ApiSuccessResponseDO(
                    rawResponse = requireNotNull(operation.responses).single(),
                    type = RefTypeDO(schemaName = "ApiResponse", nullable = false),
                ),
            annotations =
                listOf(
                    ApiAnnotationDO(
                        fqName = "io.swagger.v3.oas.annotations.Operation",
                        argsCode = listOf(operationResponsesAnnotation(modelPackage)),
                    ),
                ),
        )

    private fun createPetApi(
        operation: RawPathDO.OperationDO,
        endpoint: ApiEndpointDO,
    ): ApiDO =
        ApiDO(
            rawPath =
                RawPathDO(
                    tags = listOf("pet"),
                    operations = listOf(operation),
                ),
            generatedName = "PetApi",
            endpoints = listOf(endpoint),
        )

    private fun operationResponsesAnnotation(modelPackage: String): String =
        """
        responses = [io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "200",
          description = "Success",
          content = [io.swagger.v3.oas.annotations.media.Content(
            schema = io.swagger.v3.oas.annotations.media.Schema(implementation = $modelPackage.ApiResponse::class)
          )]
        )]
        """.trimIndent()

    private object TestSpringPolicy : ApiPolicy {
        private val responseEntity = ClassName("org.springframework.http", "ResponseEntity")

        override val suspendFunctions: Boolean = false

        override fun returnType(
            ep: ApiEndpointDO,
            ctx: TypeNameContext,
        ) = responseEntity.parameterizedBy(ep.successResponse!!.type!!.toTypeName(ctx))
    }
}
