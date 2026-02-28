package dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.model.helpers

import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.ModelAnnotationDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.ModelDO
import dev.openapi2kotlin.application.usecase.openapi2kotlin.OpenApi2KotlinUseCase

private const val SERIALIZABLE = "kotlinx.serialization.Serializable"

internal fun List<ModelDO>.handleKotlinxAnnotations(
    cfg: OpenApi2KotlinUseCase.ModelConfig.ModelAnnotationsConfig.KotlinxConfig,
) {
    if (!cfg.enabled) return

    if (cfg.serializable) {
        forEach { model ->
            model.annotations = model.annotations + ModelAnnotationDO(
                fqName = SERIALIZABLE,
            )
        }
    }
}
