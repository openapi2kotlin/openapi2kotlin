package dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.model.helpers

import dev.openapi2kotlin.application.core.openapi2kotlin.domain.model.FieldDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.model.ModelAnnotationDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.model.ModelDO
import dev.openapi2kotlin.application.usecase.openapi2kotlin.OpenApi2KotlinUseCase

private const val SERIALIZABLE = "kotlinx.serialization.Serializable"
private const val SERIAL_NAME = "kotlinx.serialization.SerialName"

internal fun List<ModelDO>.handleKotlinxAnnotations(cfg: OpenApi2KotlinUseCase.ModelConfig) {
    if (cfg.serialization != OpenApi2KotlinUseCase.ModelConfig.Serialization.KOTLINX) return

    val bySchemaName: Map<String, ModelDO> = associateBy { it.rawSchema.originalName }

    if (cfg.kotlinxSerializable) {
        forEach { model ->
            model.annotations +=
                ModelAnnotationDO(
                    fqName = SERIALIZABLE,
                )
        }
    }

    applyKotlinxPolymorphicSerialNames(bySchemaName)

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
