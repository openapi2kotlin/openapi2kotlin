package dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.api

import dev.openapi2kotlin.application.core.openapi2kotlin.model.api.ApiDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.ModelDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.raw.RawPathDO
import dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.api.helpers.toApis
import dev.openapi2kotlin.application.usecase.openapi2kotlin.OpenApi2KotlinUseCase

internal fun prepareApis(
    rawPaths: List<RawPathDO>,
    models: List<ModelDO>,
    mappingCfg: OpenApi2KotlinUseCase.ModelConfig.MappingConfig,
): List<ApiDO> {
    val ctx = ApisContext(
        modelsBySchemaName = models.associateBy { it.rawSchema.originalName },
        mappingCfg = mappingCfg,
    )

    return rawPaths.toApis(ctx)
}

internal data class ApisContext(
    val modelsBySchemaName: Map<String, ModelDO>,
    val mappingCfg: OpenApi2KotlinUseCase.ModelConfig.MappingConfig,
)
