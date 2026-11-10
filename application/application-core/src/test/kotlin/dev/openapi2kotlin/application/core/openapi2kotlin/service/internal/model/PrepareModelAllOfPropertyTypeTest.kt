package dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.model

import dev.openapi2kotlin.application.core.openapi2kotlin.domain.model.ModelDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.model.TrivialTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.raw.RawSchemaDO
import dev.openapi2kotlin.application.usecase.openapi2kotlin.OpenApi2KotlinUseCase
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class PrepareModelAllOfPropertyTypeTest {
    @Test
    fun `untyped child property inherits parent type`() {
        OpenApi2KotlinUseCase.ModelConfig.Serialization.entries.forEach { serialization ->
            val models = prepare(childType = untyped(), serialization = serialization)
            listOf("ChildLink", "ChildLinks").forEach { schema ->
                listOf("href", "rel").forEach { name ->
                    val field = models.field(schema, name)
                    val type = assertIs<TrivialTypeDO>(field.type)
                    assertEquals(TrivialTypeDO.Kind.STRING, type.kind, "$schema.$name serialization=$serialization")
                    assertEquals(true, type.nullable)
                    assertEquals(true, field.overridden)
                }
            }
        }
    }

    @Test
    fun `compatible typed child property keeps type`() {
        val href = prepare(childType = primitive(RawSchemaDO.RawPrimitiveTypeDO.Type.STRING)).field("ChildLink", "href")
        assertEquals(TrivialTypeDO.Kind.STRING, assertIs<TrivialTypeDO>(href.type).kind)
    }

    @Test
    fun `conflicting child property type fails with schema and property name`() {
        val error =
            assertFailsWith<IllegalStateException> {
                prepare(childType = primitive(RawSchemaDO.RawPrimitiveTypeDO.Type.INTEGER))
            }
        val message = error.message.orEmpty()
        assertTrue("ChildLink" in message, message)
        assertTrue("href" in message, message)
        assertTrue("ParentLink" in message, message)
    }

    private fun prepare(
        childType: RawSchemaDO.RawFieldTypeDO,
        serialization: OpenApi2KotlinUseCase.ModelConfig.Serialization =
            OpenApi2KotlinUseCase.ModelConfig.Serialization.JACKSON,
    ): List<ModelDO> =
        prepareModels(
            schemas =
                listOf(
                    schema(
                        "ParentLink",
                        ownProperties =
                            mapOf(
                                "href" to property("href", primitive(RawSchemaDO.RawPrimitiveTypeDO.Type.STRING)),
                                "rel" to property("rel", primitive(RawSchemaDO.RawPrimitiveTypeDO.Type.STRING)),
                            ),
                    ),
                    schema(
                        "ChildLink",
                        allOfParents = listOf("ParentLink"),
                        ownProperties =
                            mapOf(
                                "href" to property("href", childType),
                                "rel" to property("rel", untyped()),
                            ),
                    ),
                    schema("ChildLinks", allOfParents = listOf("ChildLink"), ownProperties = emptyMap()),
                ),
            config =
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
                ),
        )

    private fun List<ModelDO>.field(
        schema: String,
        field: String,
    ) = single { it.rawSchema.originalName == schema }.fields.single { it.originalName == field }

    private fun schema(
        name: String,
        allOfParents: List<String> = emptyList(),
        ownProperties: Map<String, RawSchemaDO.SchemaPropertyDO>,
    ) = RawSchemaDO(
        originalName = name,
        allOfParents = allOfParents,
        oneOfChildren = emptyList(),
        ownProperties = ownProperties,
        usedInPaths = true,
        discriminatorMapping = emptyMap(),
        isDiscriminatorSelfMapped = false,
    )

    private fun property(
        name: String,
        type: RawSchemaDO.RawFieldTypeDO,
    ) = RawSchemaDO.SchemaPropertyDO(name = name, type = type, required = false, defaultValue = null)

    private fun primitive(type: RawSchemaDO.RawPrimitiveTypeDO.Type) =
        RawSchemaDO.RawPrimitiveTypeDO(type = type, format = null, nullable = true)

    private fun untyped() = primitive(RawSchemaDO.RawPrimitiveTypeDO.Type.OBJECT)
}
