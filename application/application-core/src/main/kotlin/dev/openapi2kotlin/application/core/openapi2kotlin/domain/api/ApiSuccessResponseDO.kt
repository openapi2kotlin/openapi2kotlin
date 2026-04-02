package dev.openapi2kotlin.application.core.openapi2kotlin.domain.api

import dev.openapi2kotlin.application.core.openapi2kotlin.domain.model.FieldTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.raw.RawPathDO

data class ApiSuccessResponseDO(
    val rawResponse: RawPathDO.ResponseDO,
    val type: FieldTypeDO?,
)
