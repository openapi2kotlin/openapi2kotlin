package dev.openapi2kotlin.application.core.openapi2kotlin.model.server

import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.FieldTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.raw.RawPathDO

data class ServerParamDO(
    val rawParam: RawPathDO.ParamDO,
    val generatedName: String,
    val type: FieldTypeDO,
    val annotations: List<ServerAnnotationDO>,
)
