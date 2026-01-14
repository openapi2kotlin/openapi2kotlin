package dev.openapi2kotlin.adapter.generatemodel.internal.helpers

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.AnnotationSpec.UseSiteTarget
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import dev.openapi2kotlin.tools.generatortools.TypeNameContext
import dev.openapi2kotlin.tools.generatortools.toTypeName
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.FieldDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.ModelAnnotationDO
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

/**
 * Renders annotation metadata into generated Kotlin as KDoc lines.
 *
 * Kotlin/JVM annotations cannot carry arbitrary key/value pairs; metadata is therefore emitted as comments.
 *
 * Format:
 *   @meta <fqName> key1=value1; key2=value2
 */
private fun ModelAnnotationDO.toMetadataKdocLineOrNull(): String? {
    if (metadata.isEmpty()) return null

    val payload =
        metadata.entries
            .sortedBy { it.key }
            .joinToString(separator = "; ") { (k, v) ->
                val vv = v.sanitizeForKdoc()
                "$k=$vv"
            }

    return "@meta $fqName $payload"
}

private fun String.sanitizeForKdoc(): String =
    buildString {
        for (ch in this@sanitizeForKdoc) {
            when (ch) {
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(ch)
            }
        }
    }

private fun TypeSpec.Builder.addAnnotationWithMetadata(a: ModelAnnotationDO): TypeSpec.Builder {
    a.toMetadataKdocLineOrNull()?.let { addKdoc("%L\n", it) }
    addAnnotation(a.toAnnotationSpec())
    return this
}

private fun PropertySpec.Builder.addAnnotationWithMetadata(a: ModelAnnotationDO): PropertySpec.Builder {
    a.toMetadataKdocLineOrNull()?.let { addKdoc("%L\n", it) }
    addAnnotation(a.toAnnotationSpec())
    return this
}

private fun ParameterSpec.Builder.addAnnotationWithMetadata(a: ModelAnnotationDO): ParameterSpec.Builder {
    a.toMetadataKdocLineOrNull()?.let { addKdoc("%L\n", it) }
    addAnnotation(a.toAnnotationSpec())
    return this
}

private fun FunSpec.Builder.addAnnotationWithMetadata(a: ModelAnnotationDO): FunSpec.Builder {
    a.toMetadataKdocLineOrNull()?.let { addKdoc("%L\n", it) }
    addAnnotation(a.toAnnotationSpec())
    return this
}

internal fun TypeSpec.Builder.applyModelAnnotations(model: ModelDO): TypeSpec.Builder {
    for (a: ModelAnnotationDO in model.annotations) {
        addAnnotationWithMetadata(a)
    }
    return this
}

internal fun FieldDO.applyParamAnnotations(param: ParameterSpec.Builder): ParameterSpec.Builder {
    for (a: ModelAnnotationDO in annotations) {
        if (a.useSite == ModelAnnotationDO.UseSiteDO.PARAM) {
            param.addAnnotationWithMetadata(a)
        }
    }
    return param
}

internal fun FieldDO.applyPropertyAnnotations(prop: PropertySpec.Builder): PropertySpec.Builder {
    for (a: ModelAnnotationDO in annotations) {
        if (a.useSite != ModelAnnotationDO.UseSiteDO.PARAM) {
            prop.addAnnotationWithMetadata(a)
        }
    }
    return prop
}

/**
 * Applies arbitrary annotations to a PropertySpec.Builder.
 *
 * Used for enum-specific value property annotations (e.g. @get:JsonValue).
 */
internal fun PropertySpec.Builder.applyAnnotations(annotations: List<ModelAnnotationDO>): PropertySpec.Builder {
    for (a: ModelAnnotationDO in annotations) {
        addAnnotationWithMetadata(a)
    }
    return this
}

/**
 * Applies arbitrary annotations to a FunSpec.Builder.
 *
 * Used for enum-specific factory method annotations (e.g. @JsonCreator).
 */
internal fun FunSpec.Builder.applyAnnotations(annotations: List<ModelAnnotationDO>): FunSpec.Builder {
    for (a: ModelAnnotationDO in annotations) {
        addAnnotationWithMetadata(a)
    }
    return this
}

internal fun FieldDO.toParamSpec(
    ctx: TypeNameContext,
): ParameterSpec {
    val b = ParameterSpec.builder(
        generatedName,
        type.toTypeName(ctx),
    )

    applyParamAnnotations(b)

    defaultValueCode?.let { b.defaultValue("%L", it) }

    return b.build()
}