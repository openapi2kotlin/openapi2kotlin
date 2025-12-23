package dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.server

import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.ModelDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.raw.RawPathDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.server.ServerApiDO
import dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.server.helpers.enrichForKtor
import dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.server.helpers.enrichForSpring
import dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.server.helpers.toBaseServerApis
import dev.openapi2kotlin.application.usecase.openapi2kotlin.OpenApi2KotlinUseCase

internal fun prepareServerApis(
    rawPaths: List<RawPathDO>,
    models: List<ModelDO>,
    mappingCfg: OpenApi2KotlinUseCase.ModelConfig.MappingConfig,
    cfg: OpenApi2KotlinUseCase.ServerConfig,
): List<ServerApiDO> {
    val ctx = ServerPrepareContext(
        modelsBySchemaName = models.associateBy { it.rawSchema.originalName },
        mappingCfg = mappingCfg,
    )

    val base = rawPaths.toBaseServerApis(ctx)

    return when (cfg.framework) {
        OpenApi2KotlinUseCase.ServerConfig.Framework.KTOR -> base.enrichForKtor(ctx)
        OpenApi2KotlinUseCase.ServerConfig.Framework.SPRING -> base.enrichForSpring(ctx)
    }
}

internal data class ServerPrepareContext(
    val modelsBySchemaName: Map<String, ModelDO>,
    val mappingCfg: OpenApi2KotlinUseCase.ModelConfig.MappingConfig,
)
