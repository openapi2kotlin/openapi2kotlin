package dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.model

import dev.openapi2kotlin.application.core.openapi2kotlin.domain.model.ModelShapeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.raw.RawSchemaDO
import dev.openapi2kotlin.application.usecase.openapi2kotlin.OpenApi2KotlinUseCase
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs

class PrepareModelKotlinxSerializationTest {
    @Test
    fun `prepareModels removes polymorphic discriminator fields from oneOf types`() {
        val models =
            prepareModels(
                schemas =
                    listOf(
                        rawSchema(
                            originalName = "Widget",
                            oneOfChildren = listOf("BannerWidget"),
                            discriminatorPropertyName = "type",
                            discriminatorMapping =
                                mapOf(
                                    "banner-widget" to "#/components/schemas/BannerWidget",
                                ),
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

        val widget = models.single { it.rawSchema.originalName == "Widget" }
        val bannerWidget = models.single { it.rawSchema.originalName == "BannerWidget" }

        assertFalse(widget.fields.any { it.generatedName == "type" })
        assertFalse(bannerWidget.fields.any { it.generatedName == "type" })
        assertEquals(
            "kotlinx.serialization.SerialName",
            bannerWidget
                .annotations
                .single { it.argsCode == listOf("\"banner-widget\"") }
                .fqName,
        )
    }

    @Test
    fun `prepareModels removes discriminator fields from allOf ancestors`() {
        val models =
            prepareModels(
                schemas =
                    listOf(
                        rawSchema(
                            originalName = "Extensible",
                            ownProperties =
                                mapOf(
                                    "@type" to
                                        property(
                                            name = "@type",
                                            type = rawString(nullable = false),
                                            required = true,
                                        ),
                                ),
                        ),
                        rawSchema(
                            originalName = "EntityRef",
                            allOfParents = listOf("Extensible"),
                            discriminatorPropertyName = "@type",
                        ),
                        rawSchema(
                            originalName = "ProductOfferingRef",
                            allOfParents = listOf("EntityRef"),
                            discriminatorPropertyName = "@type",
                            discriminatorMapping =
                                mapOf(
                                    "ProductOfferingRef" to "#/components/schemas/ProductOfferingRef",
                                ),
                        ),
                    ),
                config = config(),
            )

        fun assertAtTypeRemoved(originalName: String) {
            assertFalse(
                models
                    .single { it.rawSchema.originalName == originalName }
                    .fields
                    .any { it.generatedName == "atType" },
            )
        }

        assertAtTypeRemoved("Extensible")
        assertAtTypeRemoved("EntityRef")
        assertAtTypeRemoved("ProductOfferingRef")
    }

    @Test
    fun `prepareModels adds SerialName to enum values with different wire values`() {
        val models =
            prepareModels(
                schemas =
                    listOf(
                        rawSchema(
                            originalName = "BannerDisplay",
                            enumValues = listOf("full-width-image", "card"),
                        ),
                    ),
                config = config(),
            )

        val enumShape =
            assertIs<ModelShapeDO.EnumClass>(
                models.single { it.rawSchema.originalName == "BannerDisplay" }.modelShape,
            )
        val fullWidthImage = enumShape.values.single { it.originalValue == "full-width-image" }
        val card = enumShape.values.single { it.originalValue == "card" }

        assertEquals("FULL_WIDTH_IMAGE", fullWidthImage.generatedValue)
        assertEquals(
            "kotlinx.serialization.SerialName",
            fullWidthImage.annotations.single { it.argsCode == listOf("\"full-width-image\"") }.fqName,
        )
        assertEquals(
            "kotlinx.serialization.SerialName",
            card.annotations.single { it.argsCode == listOf("\"card\"") }.fqName,
        )
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
        enumValues: List<String> = emptyList(),
        ownProperties: Map<String, RawSchemaDO.SchemaPropertyDO> = emptyMap(),
        discriminatorPropertyName: String? = null,
        discriminatorMapping: Map<String, String> = emptyMap(),
    ): RawSchemaDO =
        RawSchemaDO(
            originalName = originalName,
            allOfParents = allOfParents,
            oneOfChildren = oneOfChildren,
            enumValues = enumValues,
            ownProperties = ownProperties,
            discriminatorPropertyName = discriminatorPropertyName,
            usedInPaths = true,
            discriminatorMapping = discriminatorMapping,
            isDiscriminatorSelfMapped = false,
        )

    private fun property(
        name: String,
        type: RawSchemaDO.RawFieldTypeDO,
        required: Boolean,
    ): RawSchemaDO.SchemaPropertyDO =
        RawSchemaDO.SchemaPropertyDO(
            name = name,
            type = type,
            required = required,
            defaultValue = null,
        )

    private fun rawString(nullable: Boolean): RawSchemaDO.RawPrimitiveTypeDO =
        RawSchemaDO.RawPrimitiveTypeDO(
            type = RawSchemaDO.RawPrimitiveTypeDO.Type.STRING,
            nullable = nullable,
        )
}
