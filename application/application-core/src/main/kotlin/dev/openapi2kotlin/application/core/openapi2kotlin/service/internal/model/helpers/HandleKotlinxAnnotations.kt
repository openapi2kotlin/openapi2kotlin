package dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.model.helpers

import dev.openapi2kotlin.application.core.openapi2kotlin.domain.model.FieldDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.model.ModelAnnotationDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.model.ModelDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.model.ModelShapeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.model.TrivialTypeDO
import dev.openapi2kotlin.application.usecase.openapi2kotlin.OpenApi2KotlinUseCase

private const val SERIALIZABLE = "kotlinx.serialization.Serializable"
private const val SERIAL_NAME = "kotlinx.serialization.SerialName"

internal fun List<ModelDO>.handleKotlinxAnnotations(cfg: OpenApi2KotlinUseCase.ModelConfig) {
    if (cfg.serialization != OpenApi2KotlinUseCase.ModelConfig.Serialization.KOTLINX) return

    val bySchemaName: Map<String, ModelDO> = associateBy { it.rawSchema.originalName }

    if (cfg.kotlinxSerializable) {
        forEach { model ->
            val enumShape = model.modelShape as? ModelShapeDO.EnumClass
            val serializerArg =
                enumShape
                    ?.takeUnless { it.valueType.kind == TrivialTypeDO.Kind.STRING }
                    ?.let { "with = ${model.generatedName}.Serializer::class" }
            model.annotations +=
                ModelAnnotationDO(
                    fqName = SERIALIZABLE,
                    argsCode = serializerArg?.let(::listOf).orEmpty(),
                )
        }
    }

    applyKotlinxPolymorphicSerialNames(bySchemaName)
    applyKotlinxEnumSerialNames()

    forEach { model ->
        model.fields =
            model.fields
                .map { field ->
                    if (field.originalName == field.generatedName) {
                        field
                    } else {
                        field.addKotlinxAnnotation(
                            ModelAnnotationDO(
                                fqName = SERIAL_NAME,
                                argsCode = listOf("\"${field.originalName}\""),
                            ),
                        )
                    }
                }.toMutableList()
    }
}

private fun List<ModelDO>.applyKotlinxEnumSerialNames() {
    forEach { model ->
        val shape = model.modelShape as? ModelShapeDO.EnumClass ?: return@forEach
        if (shape.valueType.kind != TrivialTypeDO.Kind.STRING) return@forEach
        model.modelShape =
            shape.copy(
                values =
                    shape.values.map { enumValue ->
                        if (enumValue.originalValue == enumValue.generatedValue) {
                            enumValue
                        } else {
                            enumValue.copy(
                                annotations =
                                    enumValue.annotations +
                                        ModelAnnotationDO(
                                            fqName = SERIAL_NAME,
                                            argsCode = listOf("\"${enumValue.originalValue}\""),
                                        ),
                            )
                        }
                    },
            )
    }
}

private fun List<ModelDO>.applyKotlinxPolymorphicSerialNames(bySchemaName: Map<String, ModelDO>) {
    forEach { model ->
        val parentWithDisc = model.findNearestDiscriminatorParent(bySchemaName) ?: return@forEach
        val discriminatorValue = model.discriminatorValue(parentWithDisc)
        if (discriminatorValue == model.rawSchema.originalName) return@forEach

        model.annotations +=
            ModelAnnotationDO(
                fqName = SERIAL_NAME,
                argsCode = listOf("\"$discriminatorValue\""),
            )
    }
}

private fun FieldDO.addKotlinxAnnotation(a: ModelAnnotationDO): FieldDO = copy(annotations = annotations + a)
