package dev.openapi2kotlin.application.core.openapi2kotlin.model.api

import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.FieldTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.raw.RawPathDO

data class ApiParamDO(
    val rawParam: RawPathDO.ParamDO,
    val generatedName: String,
    val type: FieldTypeDO,
    var annotations: List<ApiAnnotationDO> = emptyList(),
)
