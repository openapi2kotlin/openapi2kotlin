package dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.model.helpers

import dev.openapi2kotlin.application.core.openapi2kotlin.domain.model.FieldDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.model.ModelAnnotationDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.model.ModelDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.model.ModelShapeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.raw.RawSchemaDO
import dev.openapi2kotlin.application.usecase.openapi2kotlin.OpenApi2KotlinUseCase

/**
 * Applies validation-related annotations to generated models.
 *
 * Responsibilities:
 *  - @Valid cascade for nested object graphs
 *  - string constraints (@Size, @Pattern)
 *  - array/list constraints (@Size)
 *  - numeric bounds (@DecimalMin, @DecimalMax)
 *
 * All constraints are derived from RawSchemaDO.SchemaPropertyDO.constraints (including inherited properties via allOf).
 */
internal fun List<ModelDO>.handleValidationAnnotations(cfg: OpenApi2KotlinUseCase.ModelConfig) {
    val validation = cfg.validation ?: return

    val bySchemaName: Map<String, ModelDO> = associateBy { it.rawSchema.originalName }

    val annotations = ValidationAnnotationNames(validation.value)

    forEach { model ->
        val useSite = model.validationUseSite()

        model.fields =
            model.fields
                .map { field ->
                    val property = model.findPropertySchemaForField(field = field, bySchemaName = bySchemaName)
                    field.applyValidationAnnotations(
                        property = property,
                        bySchemaName = bySchemaName,
                        useSite = useSite,
                        annotationNames = annotations,
                    )
                }.toMutableList()
    }
}

private fun ModelDO.validationUseSite(): ModelAnnotationDO.UseSiteDO =
    when (modelShape) {
        is ModelShapeDO.SealedInterface -> ModelAnnotationDO.UseSiteDO.GET
        else -> ModelAnnotationDO.UseSiteDO.FIELD
    }

private fun ModelDO.findPropertySchemaForField(
    field: FieldDO,
    bySchemaName: Map<String, ModelDO>,
): RawSchemaDO.SchemaPropertyDO? {
    val ownProperty = rawSchema.ownProperties[field.originalName]
    return ownProperty ?: findInheritedPropertySchema(field, bySchemaName)
}

private fun FieldDO.addValidationAnnotation(a: ModelAnnotationDO): FieldDO {
    val exists =
        annotations.any {
            it.useSite == a.useSite &&
                it.fqName == a.fqName &&
                it.argsCode == a.argsCode
        }

    return if (exists) this else copy(annotations = annotations + a)
}

private fun FieldDO.applyValidationAnnotations(
    property: RawSchemaDO.SchemaPropertyDO?,
    bySchemaName: Map<String, ModelDO>,
    useSite: ModelAnnotationDO.UseSiteDO,
    annotationNames: ValidationAnnotationNames,
): FieldDO {
    var result = this

    if (property?.required == true && result.type.nullable) {
        result =
            result.addValidationAnnotation(
                ModelAnnotationDO(
                    useSite = useSite,
                    fqName = annotationNames.notNull,
                ),
            )
    }

    if (result.shouldAddValid(bySchemaName)) {
        result =
            result.addValidationAnnotation(
                ModelAnnotationDO(
                    useSite = useSite,
                    fqName = annotationNames.valid,
                ),
            )
    }

    val constraints = property?.constraints ?: RawSchemaDO.ConstraintsDO()
    result = result.addStringConstraints(constraints, useSite, annotationNames)
    result = result.addArrayConstraints(constraints, useSite, annotationNames)
    result = result.addNumberConstraints(constraints, useSite, annotationNames)
    return result
}

private fun FieldDO.addStringConstraints(
    constraints: RawSchemaDO.ConstraintsDO,
    useSite: ModelAnnotationDO.UseSiteDO,
    annotationNames: ValidationAnnotationNames,
): FieldDO {
    var result = this
    constraints.string?.let { stringConstraints ->
        val sizeArgs =
            buildList {
                stringConstraints.minLength?.let { add("min = $it") }
                stringConstraints.maxLength?.let { add("max = $it") }
            }

        if (sizeArgs.isNotEmpty()) {
            result =
                result.addValidationAnnotation(
                    ModelAnnotationDO(
                        useSite = useSite,
                        fqName = annotationNames.size,
                        argsCode = sizeArgs,
                    ),
                )
        }

        stringConstraints.pattern?.takeIf { it.isNotBlank() }?.let { pattern ->
            result =
                result.addValidationAnnotation(
                    ModelAnnotationDO(
                        useSite = useSite,
                        fqName = annotationNames.pattern,
                        argsCode = listOf("regexp = ${pattern.toKotlinStringLiteral()}"),
                    ),
                )
        }
    }
    return result
}

private fun FieldDO.addArrayConstraints(
    constraints: RawSchemaDO.ConstraintsDO,
    useSite: ModelAnnotationDO.UseSiteDO,
    annotationNames: ValidationAnnotationNames,
): FieldDO {
    var result = this
    constraints.array?.let { arrayConstraints ->
        val sizeArgs =
            buildList {
                arrayConstraints.minItems?.let { add("min = $it") }
                arrayConstraints.maxItems?.let { add("max = $it") }
            }

        if (sizeArgs.isNotEmpty()) {
            result =
                result.addValidationAnnotation(
                    ModelAnnotationDO(
                        useSite = useSite,
                        fqName = annotationNames.size,
                        argsCode = sizeArgs,
                    ),
                )
        }
    }
    return result
}

private fun FieldDO.addNumberConstraints(
    constraints: RawSchemaDO.ConstraintsDO,
    useSite: ModelAnnotationDO.UseSiteDO,
    annotationNames: ValidationAnnotationNames,
): FieldDO {
    var result = this
    constraints.number?.let { numberConstraints ->
        numberConstraints.min?.let { min ->
            result = result.addDecimalBoundAnnotation(useSite, annotationNames.decimalMin, min)
        }
        numberConstraints.max?.let { max ->
            result = result.addDecimalBoundAnnotation(useSite, annotationNames.decimalMax, max)
        }
    }
    return result
}

private fun FieldDO.addDecimalBoundAnnotation(
    useSite: ModelAnnotationDO.UseSiteDO,
    fqName: String,
    bound: RawSchemaDO.ConstraintsDO.BoundDO,
): FieldDO =
    addValidationAnnotation(
        ModelAnnotationDO(
            useSite = useSite,
            fqName = fqName,
            argsCode =
                buildList {
                    add("value = ${bound.value.toPlainString().toKotlinStringLiteral()}")
                    if (!bound.inclusive) add("inclusive = false")
                },
        ),
    )
