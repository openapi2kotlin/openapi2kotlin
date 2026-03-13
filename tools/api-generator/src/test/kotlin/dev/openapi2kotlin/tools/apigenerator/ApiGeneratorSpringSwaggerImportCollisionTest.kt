package dev.openapi2kotlin.tools.apigenerator

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import dev.openapi2kotlin.application.core.openapi2kotlin.model.api.ApiAnnotationDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.api.ApiContextDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.api.ApiDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.api.ApiEndpointDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.api.ApiSuccessResponseDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.ModelDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.ModelShapeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.RefTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.raw.RawPathDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.raw.RawSchemaDO
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

        val apiResponseModel = ModelDO(
            rawSchema = RawSchemaDO(
                originalName = "ApiResponse",
                allOfParents = emptyList(),
                oneOfChildren = emptyList(),
                discriminatorMapping = emptyMap(),
                isDiscriminatorSelfMapped = false,
            ),
            packageName = modelPackage,
            generatedName = "ApiResponse",
            modelShape = ModelShapeDO.EmptyClass(
                extend = null,
                implements = emptyList(),
            ),
        )

        val responseType = RefTypeDO(
            schemaName = "ApiResponse",
            nullable = false,
        )

        val operation = RawPathDO.OperationDO(
            operationId = "uploadFile",
            httpMethod = RawPathDO.HttpMethodDO.POST,
            path = "/pet/{petId}/uploadImage",
            summary = "Uploads an image.",
            description = "Upload image of the pet.",
            parameters = emptyList(),
            requestBody = null,
            responses = listOf(
                RawPathDO.ResponseDO(
                    statusCode = 200,
                    type = RawSchemaDO.RawRefTypeDO("ApiResponse", false),
                ),
            ),
        )

        val endpoint = ApiEndpointDO(
            rawOperation = operation,
            generatedName = "uploadFile",
            params = emptyList(),
            requestBody = null,
            successResponse = ApiSuccessResponseDO(
                rawResponse = operation.responses!!.single(),
                type = responseType,
            ),
            annotations = listOf(
                ApiAnnotationDO(
                    fqName = "io.swagger.v3.oas.annotations.Operation",
                    argsCode = listOf(
                        """
                        responses = [io.swagger.v3.oas.annotations.responses.ApiResponse(
                          responseCode = "200",
                          description = "Success",
                          content = [io.swagger.v3.oas.annotations.media.Content(
                            schema = io.swagger.v3.oas.annotations.media.Schema(implementation = $modelPackage.ApiResponse::class)
                          )]
                        )]
                        """.trimIndent(),
                    ),
                ),
            ),
        )

        val api = ApiDO(
            rawPath = RawPathDO(
                tags = listOf("pet"),
                operations = listOf(operation),
            ),
            generatedName = "PetApi",
            endpoints = listOf(endpoint),
        )

        ApiGenerator(TestSpringPolicy).generateApi(
            GenerateApiPort.Command(
                apiContext = ApiContextDO(
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
        assertContains(generated, "import io.swagger.v3.oas.annotations.responses.ApiResponse as ResponsesApiResponseAnnotation")
        assertContains(generated, "responses = [ResponsesApiResponseAnnotation(")
        assertContains(generated, "ResponseEntity<ApiResponse>")
        assertContains(generated, "implementation = ApiResponse::class")
    }

    private object TestSpringPolicy : ApiPolicy {
        private val responseEntity = ClassName("org.springframework.http", "ResponseEntity")

        override val suspendFunctions: Boolean = false

        override fun returnType(
            ep: ApiEndpointDO,
            ctx: TypeNameContext,
        ) = responseEntity.parameterizedBy(ep.successResponse!!.type!!.toTypeName(ctx))
    }
}
