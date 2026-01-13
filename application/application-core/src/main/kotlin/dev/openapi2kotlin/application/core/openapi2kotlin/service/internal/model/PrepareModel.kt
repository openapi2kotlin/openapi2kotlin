package dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.model

import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.ModelDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.raw.RawSchemaDO
import dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.model.helpers.*
import dev.openapi2kotlin.application.usecase.openapi2kotlin.OpenApi2KotlinUseCase
import io.github.oshai.kotlinlogging.KotlinLogging

private val log = KotlinLogging.logger {}

internal fun prepareModels(schemas: List<RawSchemaDO>, config: OpenApi2KotlinUseCase.Config): List<ModelDO> {
    val models = schemas.map { ModelDO(
        rawSchema = it,
        packageName = config.model.packageName,
        generatedName = cleanSchemaNameHandler(it.originalName)
    ) }

    log.info { "Handling all of children" }
    models.handleAllOfChildren()

    log.info { "Handling parent one of" }
    models.handleParentOneOf()

    log.info { "Deciding shapes" }
    models.handleModelShape()

    log.info { "Handling fields" }
    models.handleFields(config.model.mapping)

    log.info { "Handling Jackson annotations" }
    models.handleJacksonAnnotations(
        cfg = config.model.annotations.jackson
    )

    log.info { "Handling validation annotations" }
    models.handleValidationAnnotations(
        cfg = config.model.annotations.validations
    )

    log.info { "Handling swagger annotations" }
    models.handleModelSwaggerAnnotations(
        cfg = config.api
    )

    return models
}