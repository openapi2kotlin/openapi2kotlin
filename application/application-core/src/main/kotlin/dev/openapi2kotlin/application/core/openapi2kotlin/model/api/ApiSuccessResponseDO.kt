package dev.openapi2kotlin.application.core.openapi2kotlin.model.api

import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.FieldTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.raw.RawPathDO

data class ApiSuccessResponseDO(
    val rawResponse: RawPathDO.ResponseDO,
    val type: FieldTypeDO?,
)
