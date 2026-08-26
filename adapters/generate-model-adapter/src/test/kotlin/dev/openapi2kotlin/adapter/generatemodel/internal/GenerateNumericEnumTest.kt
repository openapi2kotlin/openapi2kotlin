package dev.openapi2kotlin.adapter.generatemodel.internal

import dev.openapi2kotlin.application.core.openapi2kotlin.domain.model.EnumValueDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.model.ModelAnnotationDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.model.ModelDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.model.ModelShapeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.model.TrivialTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.raw.RawSchemaDO
import java.nio.file.Files
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertContains

class GenerateNumericEnumTest {
    @Test
    fun `generates Jackson integer enum with numeric values`() {
        val source =
            generateSource(
                model(
                    valueKind = TrivialTypeDO.Kind.INT,
                    annotations = emptyList(),
                    valueAnnotations =
                        listOf(
                            ModelAnnotationDO(
                                useSite = ModelAnnotationDO.UseSiteDO.GET,
                                fqName = "com.fasterxml.jackson.annotation.JsonValue",
                            ),
                        ),
                    fromValueAnnotations =
                        listOf(ModelAnnotationDO(fqName = "com.fasterxml.jackson.annotation.JsonCreator")),
                ),
            )

        assertContains(source, "enum class ContractDurationMonths(")
        assertContains(source, "@get:JsonValue\n    val value: Int,")
        assertContains(source, "_24(24),")
        assertContains(source, "fun fromValue(v: Int): ContractDurationMonths")
    }

    @Test
    fun `generates kotlinx serializer that writes and reads JSON numbers`() {
        val source =
            generateSource(
                model(
                    valueKind = TrivialTypeDO.Kind.LONG,
                    annotations =
                        listOf(
                            ModelAnnotationDO(
                                fqName = "kotlinx.serialization.Serializable",
                                argsCode = listOf("with = ContractDurationMonths.Serializer::class"),
                            ),
                        ),
                ),
            )

        assertContains(source, "@Serializable(with = ContractDurationMonths.Serializer::class)")
        assertContains(source, "_24(24L),")
        assertContains(source, "object Serializer : KSerializer<ContractDurationMonths>")
        assertContains(source, "PrimitiveSerialDescriptor(\"ContractDurationMonths\", PrimitiveKind.LONG)")
        assertContains(source, "encoder.encodeLong(value.value)")
        assertContains(source, "ContractDurationMonths.fromValue(decoder.decodeLong())")
    }

    private fun generateSource(model: ModelDO): String {
        val output = Files.createTempDirectory("openapi2kotlin-numeric-enum-test")
        generate(listOf(model), output)
        return output.resolve("dev/openapi2kotlin/model/ContractDurationMonths.kt").readText()
    }

    private fun model(
        valueKind: TrivialTypeDO.Kind,
        annotations: List<ModelAnnotationDO>,
        valueAnnotations: List<ModelAnnotationDO> = emptyList(),
        fromValueAnnotations: List<ModelAnnotationDO> = emptyList(),
    ): ModelDO =
        ModelDO(
            rawSchema =
                RawSchemaDO(
                    originalName = "ContractDurationMonths",
                    allOfParents = emptyList(),
                    oneOfChildren = emptyList(),
                    enumValues = listOf("24", "36", "48"),
                    discriminatorMapping = emptyMap(),
                    isDiscriminatorSelfMapped = false,
                ),
            packageName = "dev.openapi2kotlin.model",
            generatedName = "ContractDurationMonths",
            modelShape =
                ModelShapeDO.EnumClass(
                    values =
                        listOf(
                            EnumValueDO(originalValue = "24", generatedValue = "_24"),
                            EnumValueDO(originalValue = "36", generatedValue = "_36"),
                            EnumValueDO(originalValue = "48", generatedValue = "_48"),
                        ),
                    valueType = TrivialTypeDO(kind = valueKind, nullable = false),
                ),
            annotations = annotations,
            enumValueAnnotations = valueAnnotations,
            enumFromValueAnnotations = fromValueAnnotations,
        )
}
