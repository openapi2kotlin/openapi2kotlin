package dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.model

import dev.openapi2kotlin.application.core.openapi2kotlin.domain.model.ListTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.raw.RawSchemaDO
import dev.openapi2kotlin.application.usecase.openapi2kotlin.OpenApi2KotlinUseCase
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class PrepareModelTest {
    @Test
    fun `prepareModels marks fields from implemented oneOf interfaces as overrides`() {
        val models =
            prepareModels(
                schemas =
                    listOf(
                        rawSchema(
                            originalName = "Widget",
                            oneOfChildren = listOf("BannerWidget"),
                            ownProperties =
                                mapOf(
                                    "type" to
                                        property(
                                            name = "type",
                                            type = rawString(nullable = false),
                                            required = true,
                                        ),
                                ),
                        ),
                        rawSchema(
                            originalName = "BannerWidget",
                            ownProperties =
                                mapOf(
                                    "type" to
                                        property(
                                            name = "type",
                                            type = rawString(nullable = false),
                                            required = true,
                                        ),
                                ),
                        ),
                    ),
                config = config(),
            )

        val bannerTypeField =
            models
                .single { it.rawSchema.originalName == "BannerWidget" }
                .fields
                .single { it.generatedName == "type" }

        assertTrue(bannerTypeField.overridden)
    }

    @Test
    fun `prepareModels renders empty array defaults as emptyList`() {
        val models =
            prepareModels(
                schemas =
                    listOf(
                        rawSchema(
                            originalName = "Basket",
                            ownProperties =
                                mapOf(
                                    "products" to
                                        property(
                                            name = "products",
                                            type =
                                                RawSchemaDO.RawArrayTypeDO(
                                                    elementType = rawString(nullable = false),
                                                    nullable = true,
                                                ),
                                            required = false,
                                            defaultValue = "[]",
                                        ),
                                ),
                        ),
                    ),
                config = config(),
            )

        val productsField =
            models
                .single { it.rawSchema.originalName == "Basket" }
                .fields
                .single { it.generatedName == "products" }

        assertIs<ListTypeDO>(productsField.type)
        assertEquals("emptyList()", productsField.defaultValueCode)
    }

    private fun config(): OpenApi2KotlinUseCase.Config =
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
        )

    private fun rawSchema(
        originalName: String,
        allOfParents: List<String> = emptyList(),
        oneOfChildren: List<String> = emptyList(),
        ownProperties: Map<String, RawSchemaDO.SchemaPropertyDO> = emptyMap(),
    ): RawSchemaDO =
        RawSchemaDO(
            originalName = originalName,
            allOfParents = allOfParents,
            oneOfChildren = oneOfChildren,
            ownProperties = ownProperties,
            usedInPaths = true,
            discriminatorMapping = emptyMap(),
            isDiscriminatorSelfMapped = false,
        )

    private fun property(
        name: String,
        type: RawSchemaDO.RawFieldTypeDO,
        required: Boolean,
        defaultValue: String? = null,
    ): RawSchemaDO.SchemaPropertyDO =
        RawSchemaDO.SchemaPropertyDO(
            name = name,
            type = type,
            required = required,
            defaultValue = defaultValue,
        )

    private fun rawString(nullable: Boolean): RawSchemaDO.RawPrimitiveTypeDO =
        RawSchemaDO.RawPrimitiveTypeDO(
            type = RawSchemaDO.RawPrimitiveTypeDO.Type.STRING,
            nullable = nullable,
        )
}
