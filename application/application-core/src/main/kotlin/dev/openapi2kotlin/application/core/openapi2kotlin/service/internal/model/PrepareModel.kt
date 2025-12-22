package dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.model

import dev.openapi2kotlin.application.core.openapi2kotlin.model.raw.RawSchemaDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.ModelDO
import dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.model.helpers.cleanSchemaNameHandler
import dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.model.helpers.allOfChildrenHandler
import dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.model.helpers.fieldsHandler
import dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.model.helpers.modelShapeHandler
import dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.model.helpers.parentOneOfHandler
import io.github.oshai.kotlinlogging.KotlinLogging

private val log = KotlinLogging.logger {}

internal fun prepareModels(schemas: List<RawSchemaDO>, packageName: String): List<ModelDO> {
    val models = schemas.map { ModelDO(
        rawSchema = it,
        packageName = packageName,
        generatedName = cleanSchemaNameHandler(it.originalName)
    ) }

    log.info { "Handling allOfChildren" }
    allOfChildrenHandler(models)

    log.info { "Handling connection details" }
    parentOneOfHandler(models)

    log.info { "Deciding shapes" }
    modelShapeHandler(models)

    log.info { "Handling fields" }
    fieldsHandler(models)

    return models
}