package dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.api.helpers

import dev.openapi2kotlin.application.core.openapi2kotlin.model.api.ApiDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.ModelDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.raw.RawPathDO
import dev.openapi2kotlin.application.usecase.openapi2kotlin.OpenApi2KotlinUseCase
import io.github.oshai.kotlinlogging.KotlinLogging

private val log = KotlinLogging.logger {}

internal fun prepareApis(
    rawPaths: List<RawPathDO>,
    models: List<ModelDO>,
    config: OpenApi2KotlinUseCase.Config,
): List<ApiDO> {
    val ctx =
        ApisContext(
            modelsBySchemaName = models.associateBy { it.rawSchema.originalName },
            modelCfg = config.model,
            apiCfg = config.api,
        )

    val apis = rawPaths.toApis(ctx)

    log.info { "Handling swagger annotations" }
    apis.handleServerSwaggerAnnotations(
        cfg = config.api,
        ctx = ctx,
    )

    return apis
}

internal data class ApisContext(
    val modelsBySchemaName: Map<String, ModelDO>,
    val modelCfg: OpenApi2KotlinUseCase.ModelConfig,
    val apiCfg: OpenApi2KotlinUseCase.ApiConfig?,
)
