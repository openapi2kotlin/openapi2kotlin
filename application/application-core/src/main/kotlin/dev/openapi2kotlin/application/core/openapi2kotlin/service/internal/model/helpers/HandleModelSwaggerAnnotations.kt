package dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.model.helpers

import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.ModelAnnotationDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.ModelDO
import dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.common.toKotlinStringLiteral
import dev.openapi2kotlin.application.usecase.openapi2kotlin.OpenApi2KotlinUseCase

private const val SCHEMA = "io.swagger.v3.oas.annotations.media.Schema"

/**
 * Applies Swagger/OpenAPI annotations to generated models.
 *
 * Responsibilities:
 *  - type-level @Schema(name=..., description=...) for generated model types
 *
 * Notes:
 *  - name is always RawSchemaDO.originalName
 *  - description is RawSchemaDO.description (when present)
 */
internal fun List<ModelDO>.handleModelSwaggerAnnotations(
    cfg: OpenApi2KotlinUseCase.ApiConfig?,
) {
    val enabled: Boolean =
        (cfg as? OpenApi2KotlinUseCase.ApiConfig.Server)?.swagger?.enabled == true

    if (!enabled) return

    forEach { model ->
        val name = model.rawSchema.originalName
        val desc = model.rawSchema.description?.trim()?.takeIf { it.isNotBlank() }

        val argsCode = buildList {
            add("name = ${name.toKotlinStringLiteral()}")
            desc?.let { add("description = ${it.toKotlinStringLiteral()}") }
        }

        model.annotations = model.annotations + ModelAnnotationDO(
            fqName = SCHEMA,
            argsCode = argsCode,
            metadata = buildMap {
                put("swagger.schema.name", name)
                if (desc != null) put("swagger.schema.description", desc)
            },
        )
    }
}