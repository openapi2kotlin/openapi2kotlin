package dev.openapi2kotlin.adapter.generatemodel.internal.helpers

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.AnnotationSpec.UseSiteTarget
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.ModelAnnotationDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.FieldDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.ModelDO

internal fun ModelAnnotationDO.toAnnotationSpec(): AnnotationSpec {
    val (pkg, simple) = fqName.splitPackageAndSimple()
    val cls = ClassName(pkg, simple)

    val b = AnnotationSpec.builder(cls)

    val target = when (useSite) {
        ModelAnnotationDO.UseSiteDO.NONE -> null
        ModelAnnotationDO.UseSiteDO.PARAM -> UseSiteTarget.PARAM
        ModelAnnotationDO.UseSiteDO.GET -> UseSiteTarget.GET
        ModelAnnotationDO.UseSiteDO.SET -> UseSiteTarget.SET
        ModelAnnotationDO.UseSiteDO.FIELD -> UseSiteTarget.FIELD
    }
    if (target != null) b.useSiteTarget(target)

    for (arg in argsCode) {
        b.addMember("%L", arg)
    }

    return b.build()
}

private fun String.splitPackageAndSimple(): Pair<String, String> {
    val idx = lastIndexOf('.')
    require(idx > 0 && idx < length - 1) { "fqName must be fully-qualified, got: '$this'" }
    return substring(0, idx) to substring(idx + 1)
}

internal fun TypeSpec.Builder.applyModelAnnotations(model: ModelDO): TypeSpec.Builder {
    for (a: ModelAnnotationDO in model.annotations) {
        addAnnotation(a.toAnnotationSpec())
    }
    return this
}

internal fun FieldDO.applyParamAnnotations(param: ParameterSpec.Builder): ParameterSpec.Builder {
    for (a: ModelAnnotationDO in annotations) {
        if (a.useSite == ModelAnnotationDO.UseSiteDO.PARAM) {
            param.addAnnotation(a.toAnnotationSpec())
        }
    }
    return param
}

internal fun FieldDO.applyPropertyAnnotations(prop: PropertySpec.Builder): PropertySpec.Builder {
    for (a: ModelAnnotationDO in annotations) {
        if (a.useSite != ModelAnnotationDO.UseSiteDO.PARAM) {
            prop.addAnnotation(a.toAnnotationSpec())
        }
    }
    return prop
}

internal fun FieldDO.toParamSpec(
    owner: ModelDO,
    bySchemaName: Map<String, ModelDO>,
): ParameterSpec {
    val b = ParameterSpec.builder(
        generatedName,
        type.typeName(owner, bySchemaName),
    )

    applyParamAnnotations(b)

    defaultValueCode?.let { b.defaultValue("%L", it) }

    return b.build()
}