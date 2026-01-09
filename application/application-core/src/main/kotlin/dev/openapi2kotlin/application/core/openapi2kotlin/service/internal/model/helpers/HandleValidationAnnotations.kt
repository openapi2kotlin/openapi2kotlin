package dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.model.helpers

import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.FieldDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.ListTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.ModelAnnotationDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.ModelDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.ModelShapeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.RefTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.raw.RawSchemaDO
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
internal fun List<ModelDO>.handleValidationAnnotations(
    cfg: OpenApi2KotlinUseCase.ModelConfig.AnnotationsConfig.ValidationAnnotationsConfig,
) {
    if (!cfg.enabled) return

    val bySchemaName: Map<String, ModelDO> = associateBy { it.rawSchema.originalName }

    val ns = cfg.namespace.value
    val VALID = "$ns.validation.Valid"
    val NOT_NULL = "$ns.validation.constraints.NotNull"
    val SIZE = "$ns.validation.constraints.Size"
    val PATTERN = "$ns.validation.constraints.Pattern"
    val DECIMAL_MIN = "$ns.validation.constraints.DecimalMin"
    val DECIMAL_MAX = "$ns.validation.constraints.DecimalMax"

    forEach { model ->
        val useSite = model.validationUseSite()

        model.fields = model.fields.map { f ->
            val prop = model.findPropertySchemaForField(field = f, bySchemaName = bySchemaName)

            var out = f

            // Required-but-nullable edge case (schema: nullable=true + required=true)
            if (prop?.required == true && out.type.nullable) {
                out = out.addAnnotation(
                    ModelAnnotationDO(
                        useSite = useSite,
                        fqName = NOT_NULL,
                    )
                )
            }

            // Cascade validation for nested objects and lists of objects
            if (out.shouldAddValid(bySchemaName)) {
                out = out.addAnnotation(
                    ModelAnnotationDO(
                        useSite = useSite,
                        fqName = VALID,
                    )
                )
            }

            val constraints = prop?.constraints ?: RawSchemaDO.ConstraintsDO()

            // String constraints (minLength/maxLength/pattern)
            constraints.string?.let { sc ->
                val sizeArgs = buildList {
                    sc.minLength?.let { add("min = $it") }
                    sc.maxLength?.let { add("max = $it") }
                }
                if (sizeArgs.isNotEmpty()) {
                    out = out.addAnnotation(
                        ModelAnnotationDO(
                            useSite = useSite,
                            fqName = SIZE,
                            argsCode = sizeArgs,
                        )
                    )
                }

                sc.pattern?.takeIf { it.isNotBlank() }?.let { p ->
                    out = out.addAnnotation(
                        ModelAnnotationDO(
                            useSite = useSite,
                            fqName = PATTERN,
                            argsCode = listOf("regexp = ${p.toKotlinStringLiteral()}"),
                        )
                    )
                }
            }

            // Array/List constraints (minItems/maxItems)
            constraints.array?.let { ac ->
                val sizeArgs = buildList {
                    ac.minItems?.let { add("min = $it") }
                    ac.maxItems?.let { add("max = $it") }
                }
                if (sizeArgs.isNotEmpty()) {
                    out = out.addAnnotation(
                        ModelAnnotationDO(
                            useSite = useSite,
                            fqName = SIZE,
                            argsCode = sizeArgs,
                        )
                    )
                }
            }

            // Numeric constraints (minimum/maximum inclusive/exclusive)
            constraints.number?.let { nc ->
                nc.min?.let { min ->
                    out = out.addAnnotation(
                        ModelAnnotationDO(
                            useSite = useSite,
                            fqName = DECIMAL_MIN,
                            argsCode = buildList {
                                add("value = ${min.value.toPlainString().toKotlinStringLiteral()}")
                                if (!min.inclusive) add("inclusive = false")
                            },
                        )
                    )
                }

                nc.max?.let { max ->
                    out = out.addAnnotation(
                        ModelAnnotationDO(
                            useSite = useSite,
                            fqName = DECIMAL_MAX,
                            argsCode = buildList {
                                add("value = ${max.value.toPlainString().toKotlinStringLiteral()}")
                                if (!max.inclusive) add("inclusive = false")
                            },
                        )
                    )
                }
            }

            out
        }.toMutableList()
    }
}

private fun ModelDO.validationUseSite(): ModelAnnotationDO.UseSiteDO =
    when (modelShape) {
        is ModelShapeDO.SealedInterface -> ModelAnnotationDO.UseSiteDO.GET
        else -> ModelAnnotationDO.UseSiteDO.FIELD
    }

private fun FieldDO.shouldAddValid(bySchemaName: Map<String, ModelDO>): Boolean {
    return when (val t = type) {
        is RefTypeDO -> {
            val target = bySchemaName[t.schemaName]?.rawSchema ?: return true
            // Enums are terminal; @Valid is redundant.
            target.enumValues.isEmpty()
        }

        is ListTypeDO -> {
            when (val e = t.elementType) {
                is RefTypeDO -> {
                    val target = bySchemaName[e.schemaName]?.rawSchema ?: return true
                    target.enumValues.isEmpty()
                }

                else -> false
            }
        }

        else -> false
    }
}

private fun ModelDO.findPropertySchemaForField(
    field: FieldDO,
    bySchemaName: Map<String, ModelDO>,
): RawSchemaDO.SchemaPropertyDO? {
    rawSchema.ownProperties[field.originalName]?.let { return it }

    val visited = mutableSetOf<String>()
    val queue = ArrayDeque(rawSchema.allOfParents)

    while (queue.isNotEmpty()) {
        val parentName = queue.removeFirst()
        if (!visited.add(parentName)) continue

        val parent = bySchemaName[parentName] ?: continue
        parent.rawSchema.ownProperties[field.originalName]?.let { return it }

        queue.addAll(parent.rawSchema.allOfParents)
    }

    return null
}

private fun FieldDO.addAnnotation(a: ModelAnnotationDO): FieldDO {
    val exists =
        annotations.any {
            it.useSite == a.useSite &&
                    it.fqName == a.fqName &&
                    it.argsCode == a.argsCode
        }

    return if (exists) this else copy(annotations = annotations + a)
}

private fun String.toKotlinStringLiteral(): String =
    buildString {
        append('"')
        for (ch in this@toKotlinStringLiteral) {
            when (ch) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(ch)
            }
        }
        append('"')
    }
