package dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.model

import dev.openapi2kotlin.application.core.openapi2kotlin.domain.model.ListTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.model.MapTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.model.ModelDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.model.ModelShapeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.model.RefTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.model.TrivialTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.raw.RawSchemaDO
import dev.openapi2kotlin.application.usecase.openapi2kotlin.OpenApi2KotlinUseCase
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class PrepareModelTest {
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

    @Test
    fun `prepareModels keeps typed object additionalProperties as map value type`() {
        val models =
            prepareModels(
                schemas =
                    listOf(
                        rawSchema(
                            originalName = "ScreenResult",
                            ownProperties =
                                mapOf(
                                    "slots" to
                                        property(
                                            name = "slots",
                                            type =
                                                RawSchemaDO.RawMapTypeDO(
                                                    valueType =
                                                        RawSchemaDO.RawRefTypeDO(
                                                            schemaName = "Layout",
                                                            nullable = false,
                                                        ),
                                                    nullable = false,
                                                ),
                                            required = true,
                                        ),
                                ),
                        ),
                        rawSchema(originalName = "Layout"),
                    ),
                config = config(),
            )

        val slotsField =
            models
                .single { it.rawSchema.originalName == "ScreenResult" }
                .fields
                .single { it.generatedName == "slots" }

        val mapType = assertIs<MapTypeDO>(slotsField.type)
        val valueType = assertIs<RefTypeDO>(mapType.valueType)

        assertEquals(false, mapType.nullable)
        assertEquals("Layout", valueType.schemaName)
        assertEquals(false, valueType.nullable)
    }

    @Test
    fun `prepareModels renders enum ref defaults as enum constants`() {
        val models =
            prepareModels(
                schemas =
                    listOf(
                        rawSchema(
                            originalName = "ButtonWidget",
                            ownProperties =
                                mapOf(
                                    "style" to
                                        property(
                                            name = "style",
                                            type =
                                                RawSchemaDO.RawRefTypeDO(
                                                    schemaName = "ButtonStyle",
                                                    nullable = true,
                                                ),
                                            required = false,
                                            defaultValue = "primary",
                                        ),
                                ),
                        ),
                        rawSchema(
                            originalName = "ButtonStyle",
                            enumValues = listOf("primary", "secondary"),
                        ),
                    ),
                config = config(),
            )

        val styleField =
            models
                .single { it.rawSchema.originalName == "ButtonWidget" }
                .fields
                .single { it.generatedName == "style" }

        assertEquals("ButtonStyle.PRIMARY", styleField.defaultValueCode)
    }

    @Test
    fun `prepareModels retains integer enum wire type for Jackson`() {
        val model =
            prepareModels(
                schemas =
                    listOf(
                        rawSchema(
                            originalName = "ContractDurationMonths",
                            enumValues = listOf("24", "36", "48"),
                            enumType =
                                RawSchemaDO.RawPrimitiveTypeDO(
                                    type = RawSchemaDO.RawPrimitiveTypeDO.Type.INTEGER,
                                    nullable = false,
                                ),
                        ),
                    ),
                config = config(OpenApi2KotlinUseCase.ModelConfig.Serialization.JACKSON),
            ).single()

        val shape = assertIs<ModelShapeDO.EnumClass>(model.modelShape)
        assertEquals(
            TrivialTypeDO.Kind.LONG,
            shape.valueType.kind,
        )
        assertEquals("com.fasterxml.jackson.annotation.JsonValue", model.enumValueAnnotations.single().fqName)
        assertEquals("com.fasterxml.jackson.annotation.JsonCreator", model.enumFromValueAnnotations.single().fqName)
    }

    @Test
    fun `prepareModels moves wider oneOf parent to narrower interface`() {
        val models =
            prepareModels(
                schemas =
                    listOf(
                        rawSchema(
                            originalName = "Widget",
                            oneOfChildren = listOf("BannerWidget", "ProductCardWidget", "TextWidget"),
                            discriminatorPropertyName = "type",
                            discriminatorMapping =
                                mapOf(
                                    "banner" to "#/components/schemas/BannerWidget",
                                    "product-card" to "#/components/schemas/ProductCardWidget",
                                    "text" to "#/components/schemas/TextWidget",
                                ),
                        ),
                        rawSchema(
                            originalName = "SlotItem",
                            oneOfChildren = listOf("BannerWidget", "ProductCardWidget"),
                            discriminatorPropertyName = "type",
                            discriminatorMapping =
                                mapOf(
                                    "banner" to "#/components/schemas/BannerWidget",
                                    "product-card" to "#/components/schemas/ProductCardWidget",
                                ),
                        ),
                        rawSchema(originalName = "BannerWidget"),
                        rawSchema(originalName = "ProductCardWidget"),
                        rawSchema(originalName = "TextWidget"),
                    ),
                config = config(),
            )

        val widgetShape = assertIs<ModelShapeDO.SealedInterface>(models.shapeOf("Widget"))
        val slotItemShape = assertIs<ModelShapeDO.SealedInterface>(models.shapeOf("SlotItem"))
        val bannerShape = assertIs<ModelShapeDO.EmptyClass>(models.shapeOf("BannerWidget"))
        val textShape = assertIs<ModelShapeDO.EmptyClass>(models.shapeOf("TextWidget"))

        assertEquals(emptyList(), widgetShape.extends)
        assertEquals(listOf("Widget"), slotItemShape.extends)
        assertEquals(listOf("SlotItem"), bannerShape.implements)
        assertEquals(listOf("Widget"), textShape.implements)
    }

    private fun config(
        serialization: OpenApi2KotlinUseCase.ModelConfig.Serialization =
            OpenApi2KotlinUseCase.ModelConfig.Serialization.KOTLINX,
    ): OpenApi2KotlinUseCase.Config =
        OpenApi2KotlinUseCase.Config(
            inputSpecPath = Paths.get("openapi.yaml"),
            outputDirPath = Paths.get("build/generated"),
            model =
                OpenApi2KotlinUseCase.ModelConfig(
                    packageName = "dev.openapi2kotlin.model",
                    serialization = serialization,
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
        enumType: RawSchemaDO.RawPrimitiveTypeDO? = null,
        ownProperties: Map<String, RawSchemaDO.SchemaPropertyDO> = emptyMap(),
        discriminatorPropertyName: String? = null,
        discriminatorMapping: Map<String, String> = emptyMap(),
    ): RawSchemaDO =
        RawSchemaDO(
            originalName = originalName,
            allOfParents = allOfParents,
            oneOfChildren = oneOfChildren,
            enumValues = enumValues,
            enumType = enumType,
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

    private fun List<ModelDO>.shapeOf(originalName: String): ModelShapeDO =
        single { it.rawSchema.originalName == originalName }.modelShape
}
