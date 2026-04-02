package dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.model.helpers

import dev.openapi2kotlin.application.core.openapi2kotlin.domain.model.ModelDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.model.ModelShapeDO

internal fun ModelDO.hasConcreteClassShape(): Boolean =
    modelShape is ModelShapeDO.OpenClass ||
        modelShape is ModelShapeDO.DataClass ||
        modelShape is ModelShapeDO.EmptyClass
