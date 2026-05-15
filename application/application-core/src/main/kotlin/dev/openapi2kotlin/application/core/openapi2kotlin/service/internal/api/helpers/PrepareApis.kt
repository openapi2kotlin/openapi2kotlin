package dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.api.helpers

import dev.openapi2kotlin.application.core.openapi2kotlin.domain.api.ApiDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.model.ModelDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.raw.RawPathDO
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

    val preparedRawPaths = rawPaths.groupForApiNaming(apiCfg = config.api)
    val apis = preparedRawPaths.toApis(ctx)

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

private fun List<RawPathDO>.groupForApiNaming(apiCfg: OpenApi2KotlinUseCase.ApiConfig?): List<RawPathDO> {
    if (apiCfg?.apiNameFromTags != true) return this

    val groupedOperations = linkedMapOf<String, MutableList<RawPathDO.OperationDO>>()

    flatMap { rawPath -> rawPath.operations }
        .forEach { operation ->
            val primaryTag = operation.tags.firstOrNull().orEmpty().ifBlank { "Default" }
            groupedOperations.getOrPut(primaryTag) { mutableListOf() }.add(operation)
        }

    return groupedOperations.entries
        .map { (tag, operations) ->
            RawPathDO(
                tags = listOf(tag),
                operations =
                    operations.sortedWith(
                        compareBy<RawPathDO.OperationDO> { it.operationId ?: "" }
                            .thenBy { it.httpMethod.name }
                            .thenBy { it.path },
                    ),
            )
        }.sortedBy { it.tags.firstOrNull().orEmpty() }
}
