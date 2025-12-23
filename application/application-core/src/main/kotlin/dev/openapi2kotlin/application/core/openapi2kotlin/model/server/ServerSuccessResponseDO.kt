package dev.openapi2kotlin.application.core.openapi2kotlin.model.server

import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.FieldTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.raw.RawPathDO

data class ServerSuccessResponseDO(
    val rawResponse: RawPathDO.ResponseDO,
    val type: FieldTypeDO?,
)
