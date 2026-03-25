package dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.model.helpers

import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.PolymorphismDO

internal data class PolymorphismMetadata(
    val polymorphism: PolymorphismDO,
    val childrenSchemaNames: List<String>,
    val isOneOfWrapperSealedInterface: Boolean,
)
