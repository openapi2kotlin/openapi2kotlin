package dev.openapi2kotlin.application.core.openapi2kotlin.domain.api

import dev.openapi2kotlin.application.core.openapi2kotlin.domain.model.FieldTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.raw.RawPathDO

data class ApiParamDO(
    val rawParam: RawPathDO.ParamDO,
    val generatedName: String,
    val type: FieldTypeDO,
    var annotations: List<ApiAnnotationDO> = emptyList(),
)
