package dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.api

import dev.openapi2kotlin.application.core.openapi2kotlin.model.api.ApiContextDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.ModelDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.raw.RawPathDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.raw.RawServerDO
import dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.api.helpers.prepareApis
import dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.api.helpers.prepareBasePath
import dev.openapi2kotlin.application.usecase.openapi2kotlin.OpenApi2KotlinUseCase

fun prepareApiContext(
    rawPaths: List<RawPathDO>,
    rawServers: List<RawServerDO>,
    models: List<ModelDO>,
    config: OpenApi2KotlinUseCase.Config,
): ApiContextDO {
    val apis = prepareApis(
        rawPaths = rawPaths,
        models = models,
        config = config,
    )

    val basePath = prepareBasePath(
        rawServers = rawServers,
        config = config,
    )

    return ApiContextDO(
        apis = apis,
        basePath = basePath,
    )
}