package dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.server.helpers

import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.FieldTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.raw.RawSchemaDO
import dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.common.toFinalType
import dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.common.withNullability
import dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.server.ServerPrepareContext

internal fun RawSchemaDO.RawFieldTypeDO.toServerFieldType(
    ctx: ServerPrepareContext,
    required: Boolean,
): FieldTypeDO {
    val base = toFinalType(cfg = ctx.mappingCfg)
    val finalNullable = base.nullable || !required
    return base.withNullability(finalNullable)
}
