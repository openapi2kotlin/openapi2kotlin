package dev.openapi2kotlin.adapter.parser

import dev.openapi2kotlin.application.core.openapi2kotlin.domain.raw.RawSchemaDO
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ParseSpecAdapterTest {
    private val adapter = ParseSpecAdapter()

    @Test
    fun `parseSpec keeps primitive and ref types for openapi 3_1_1`() {
        val result = adapter.parseSpec(resourcePath("openapi-3.1.1.yaml"))

        val translationRequest =
            result.rawSchemas
                .single { it.originalName == "TranslationRequest" }

        val properties = translationRequest.ownProperties

        assertPrimitive(properties.getValue("lang").type, RawSchemaDO.RawPrimitiveTypeDO.Type.STRING, nullable = false)
        assertPrimitive(
            properties.getValue("attempts").type,
            RawSchemaDO.RawPrimitiveTypeDO.Type.INTEGER,
            nullable = false,
        )
        assertPrimitive(
            properties.getValue("active").type,
            RawSchemaDO.RawPrimitiveTypeDO.Type.BOOLEAN,
            nullable = false,
        )

        val aliasesType = assertIs<RawSchemaDO.RawArrayTypeDO>(properties.getValue("aliases").type)
        assertEquals(false, aliasesType.nullable)
        assertPrimitive(aliasesType.elementType, RawSchemaDO.RawPrimitiveTypeDO.Type.STRING, nullable = false)

        val productType = assertIs<RawSchemaDO.RawRefTypeDO>(properties.getValue("product").type)
        assertEquals("Product", productType.schemaName)
        assertEquals(false, productType.nullable)

        val actionType = assertIs<RawSchemaDO.RawRefTypeDO>(properties.getValue("action").type)
        assertEquals("ItemAction", actionType.schemaName)
        assertEquals(true, actionType.nullable)

        val styleType = assertIs<RawSchemaDO.RawRefTypeDO>(properties.getValue("style").type)
        assertEquals("WidgetStyle", styleType.schemaName)
        assertEquals(true, styleType.nullable)

        val displayType = assertIs<RawSchemaDO.RawRefTypeDO>(properties.getValue("display").type)
        assertEquals("TranslationRequestDisplay", displayType.schemaName)
        assertEquals(true, displayType.nullable)

        val slotsType = assertIs<RawSchemaDO.RawMapTypeDO>(properties.getValue("slots").type)
        assertEquals(true, slotsType.nullable)
        val slotValueType = assertIs<RawSchemaDO.RawRefTypeDO>(slotsType.valueType)
        assertEquals("Layout", slotValueType.schemaName)
        assertEquals(false, slotValueType.nullable)

        assertPrimitive(
            properties.getValue("optionalNote").type,
            RawSchemaDO.RawPrimitiveTypeDO.Type.STRING,
            nullable = true,
        )

        val displayEnum = result.rawSchemas.single { it.originalName == "TranslationRequestDisplay" }
        assertEquals(listOf("compact", "expanded"), displayEnum.enumValues)
    }

    @Test
    fun `parseSpec preserves actual operation tags for regrouping`() {
        val result = adapter.parseSpec(resourcePath("openapi-3.1.1.yaml"))

        val operationTags =
            result.rawPaths
                .flatMap { rawPath -> rawPath.operations }
                .associate { operation -> operation.operationId to operation.tags }

        assertEquals(listOf("ProductCatalog"), operationTags.getValue("translateProducts"))
        assertEquals(listOf("Coupons"), operationTags.getValue("translateCoupons"))
        assertTrue(operationTags.getValue("health").isEmpty())
    }

    private fun resourcePath(resourceName: String) =
        Paths.get(
            checkNotNull(javaClass.classLoader.getResource(resourceName)) {
                "Missing test resource: $resourceName"
            }.toURI(),
        )

    private fun assertPrimitive(
        fieldType: RawSchemaDO.RawFieldTypeDO,
        expectedType: RawSchemaDO.RawPrimitiveTypeDO.Type,
        nullable: Boolean,
    ) {
        val primitiveType = assertIs<RawSchemaDO.RawPrimitiveTypeDO>(fieldType)
        assertEquals(expectedType, primitiveType.type)
        assertEquals(nullable, primitiveType.nullable)
    }
}
