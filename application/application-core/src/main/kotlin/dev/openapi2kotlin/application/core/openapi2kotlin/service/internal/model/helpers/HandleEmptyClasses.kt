package dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.model.helpers

import dev.openapi2kotlin.application.core.openapi2kotlin.domain.model.ModelDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.model.ModelShapeDO

internal fun List<ModelDO>.handleEmptyClasses() {
    forEach { model ->
        val shape = model.modelShape as? ModelShapeDO.DataClass ?: return@forEach
        if (model.fields.isNotEmpty()) return@forEach

        model.modelShape =
            ModelShapeDO.EmptyClass(
                extend = shape.extend,
                implements = shape.implements,
            )
    }
}
