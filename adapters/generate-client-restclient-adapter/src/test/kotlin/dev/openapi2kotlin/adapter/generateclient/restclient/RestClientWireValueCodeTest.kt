package dev.openapi2kotlin.adapter.generateclient.restclient

import dev.openapi2kotlin.application.core.openapi2kotlin.domain.model.EnumValueDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.model.ModelDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.model.ModelShapeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.model.RefTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.model.TrivialTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.raw.RawSchemaDO
import dev.openapi2kotlin.tools.generatortools.TypeNameContext
import kotlin.test.Test
import kotlin.test.assertEquals

class RestClientWireValueCodeTest {
    @Test
    fun `uses declared wire value for referenced enum parameters`() {
        val ownerChannel =
            ModelDO(
                rawSchema = rawSchema("OwnerChannel"),
                packageName = "example.model",
                generatedName = "OwnerChannel",
                modelShape =
                    ModelShapeDO.EnumClass(
                        values =
                            listOf(
                                EnumValueDO(
                                    originalValue = "CUSTOMER_SERVICE_B2B",
                                    generatedValue = "CUSTOMER_SERVICE_B_2_B",
                                ),
                            ),
                        valueType = TrivialTypeDO(TrivialTypeDO.Kind.STRING, nullable = false),
                    ),
            )
        val context = TypeNameContext("example.model", mapOf("OwnerChannel" to ownerChannel))

        assertEquals(
            "ownerChannel.value.toString()",
            RefTypeDO(schemaName = "OwnerChannel", nullable = false)
                .toRestClientWireValueCode("ownerChannel", context),
        )
    }

    @Test
    fun `keeps strings unchanged and stringifies other scalar values`() {
        val context = TypeNameContext("example.model")

        assertEquals(
            "text",
            TrivialTypeDO(TrivialTypeDO.Kind.STRING, nullable = false)
                .toRestClientWireValueCode("text", context),
        )
        assertEquals(
            "count.toString()",
            TrivialTypeDO(TrivialTypeDO.Kind.INT, nullable = false)
                .toRestClientWireValueCode("count", context),
        )
    }

    private fun rawSchema(name: String): RawSchemaDO =
        RawSchemaDO(
            originalName = name,
            allOfParents = emptyList(),
            oneOfChildren = emptyList(),
            discriminatorMapping = emptyMap(),
            isDiscriminatorSelfMapped = false,
        )
}
