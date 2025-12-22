package dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.model.helpers

import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.FieldTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.ListTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.PrimitiveTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.RefTypeDO

internal fun FieldTypeDO.withNullability(nullable: Boolean): FieldTypeDO = when (this) {
    is PrimitiveTypeDO -> copy(nullable = nullable)
    is RefTypeDO -> copy(nullable = nullable)
    is ListTypeDO -> copy(nullable = nullable)
}