package dev.openapi2kotlin.adapter.generatemodel.internal.helpers

import com.squareup.kotlinpoet.ParameterSpec
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.model.FieldDO
import dev.openapi2kotlin.tools.generatortools.TypeNameContext
import dev.openapi2kotlin.tools.generatortools.toTypeName

internal fun FieldDO.toParamSpec(
    ctx: TypeNameContext,
    renderOverriddenInCtorOnly: Boolean = false,
): ParameterSpec {
    val b =
        ParameterSpec.builder(
            if (renderOverriddenInCtorOnly && overridden) "${generatedName}_" else generatedName,
            type.toTypeName(ctx),
        )

    applyParamAnnotations(b)

    defaultValueCode?.let { b.defaultValue("%L", it) }

    return b.build()
}
