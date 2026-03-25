package dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.model.helpers

import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.ModelDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.ModelShapeDO

internal fun ModelDO.hasConcreteClassShape(): Boolean =
    modelShape is ModelShapeDO.OpenClass ||
        modelShape is ModelShapeDO.DataClass ||
        modelShape is ModelShapeDO.EmptyClass
