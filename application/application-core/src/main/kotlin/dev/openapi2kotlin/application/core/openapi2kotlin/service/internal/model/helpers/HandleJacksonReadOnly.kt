package dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.model.helpers

import dev.openapi2kotlin.application.core.openapi2kotlin.domain.model.FieldDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.model.ModelAnnotationDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.model.ModelDO

internal fun ModelDO.resolveDiscriminatorName(bySchemaName: Map<String, ModelDO>): String? =
    rawSchema.discriminatorPropertyName
        ?: findNearestDiscriminatorParent(bySchemaName)?.rawSchema?.discriminatorPropertyName

internal fun FieldDO.withReadOnlyDiscriminator(discriminatorName: String): FieldDO =
    if (originalName != discriminatorName) {
        this
    } else {
        addAnnotation(
            ModelAnnotationDO(
                useSite = ModelAnnotationDO.UseSiteDO.GET,
                fqName = JSON_PROPERTY,
                argsCode =
                    listOf(
                        "value = \"$discriminatorName\"",
                        "access = JsonProperty.Access.READ_ONLY",
                    ),
            ),
        )
    }
