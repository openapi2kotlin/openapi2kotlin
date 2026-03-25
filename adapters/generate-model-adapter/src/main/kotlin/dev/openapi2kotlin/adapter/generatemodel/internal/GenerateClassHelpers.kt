package dev.openapi2kotlin.adapter.generatemodel.internal

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import dev.openapi2kotlin.adapter.generatemodel.internal.helpers.applyPropertyAnnotations
import dev.openapi2kotlin.adapter.generatemodel.internal.helpers.toParamSpec
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.FieldDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.ModelDO
import dev.openapi2kotlin.tools.generatortools.TypeNameContext
import dev.openapi2kotlin.tools.generatortools.toTypeName

private val KOTLINX_TRANSIENT = ClassName("kotlinx.serialization", "Transient")

internal fun FieldDO.storageName(renderOverriddenInCtorOnly: Boolean): String =
    if (renderOverriddenInCtorOnly && overridden) "${generatedName}_" else generatedName

internal fun buildClassConstructor(
    schema: ModelDO,
    ctx: TypeNameContext,
    renderOverriddenInCtorOnly: Boolean,
): FunSpec =
    FunSpec.constructorBuilder().apply {
        schema.fields.forEach { field ->
            addParameter(field.toParamSpec(ctx, renderOverriddenInCtorOnly))
        }
    }.build()

internal fun TypeSpec.Builder.addModelKdoc(schema: ModelDO): TypeSpec.Builder {
    schema.kdoc?.takeIf { it.isNotBlank() }?.let { doc ->
        addKdoc("%L\n", doc.trim())
    }
    return this
}

internal fun addSchemaProperties(
    typeBuilder: TypeSpec.Builder,
    schema: ModelDO,
    ctx: TypeNameContext,
    renderOverriddenInCtorOnly: Boolean,
    shouldOpenProps: Boolean,
) {
    schema.fields.forEach { field ->
        addSchemaProperty(
            typeBuilder = typeBuilder,
            field = field,
            ctx = ctx,
            renderOverriddenInCtorOnly = renderOverriddenInCtorOnly,
            shouldOpenProps = shouldOpenProps,
        )
    }
}

private fun addSchemaProperty(
    typeBuilder: TypeSpec.Builder,
    field: FieldDO,
    ctx: TypeNameContext,
    renderOverriddenInCtorOnly: Boolean,
    shouldOpenProps: Boolean,
) {
    val storageName = field.storageName(renderOverriddenInCtorOnly)

    if (renderOverriddenInCtorOnly && field.overridden) {
        addCtorOnlyOverrideProperty(typeBuilder, field, ctx, storageName)
        return
    }

    val propBuilder =
        PropertySpec.builder(
            storageName,
            field.type.toTypeName(ctx),
        ).initializer(storageName)

    field.kdoc?.takeIf { it.isNotBlank() }?.let { doc ->
        propBuilder.addKdoc("%L\n", doc.trim())
    }

    field.applyPropertyAnnotations(propBuilder)

    if (field.overridden) {
        propBuilder.addModifiers(KModifier.OVERRIDE)
    } else if (shouldOpenProps) {
        propBuilder.addModifiers(KModifier.OPEN)
    }

    typeBuilder.addProperty(propBuilder.build())
}

private fun addCtorOnlyOverrideProperty(
    typeBuilder: TypeSpec.Builder,
    field: FieldDO,
    ctx: TypeNameContext,
    storageName: String,
) {
    typeBuilder.addProperty(
        PropertySpec.builder(
            storageName,
            field.type.toTypeName(ctx),
        ).initializer(storageName)
            .addModifiers(KModifier.PRIVATE)
            .apply {
                field.applyPropertyAnnotations(this)
            }
            .build(),
    )

    val overrideProperty =
        PropertySpec.builder(
            field.generatedName,
            field.type.toTypeName(ctx),
        ).getter(
            FunSpec.getterBuilder()
                .addCode("return %N\n", storageName)
                .build(),
        ).addModifiers(KModifier.OVERRIDE)
            .addAnnotation(AnnotationSpec.builder(KOTLINX_TRANSIENT).build())

    field.kdoc?.takeIf { it.isNotBlank() }?.let { doc ->
        overrideProperty.addKdoc("%L\n", doc.trim())
    }

    typeBuilder.addProperty(overrideProperty.build())
}

internal fun addParentInheritance(
    typeBuilder: TypeSpec.Builder,
    schema: ModelDO,
    extend: String?,
    byName: Map<String, ModelDO>,
    renderOverriddenInCtorOnly: Boolean,
) {
    extend?.let { parentName ->
        val parent = byName[parentName]
        val typeName = parent?.className() ?: ClassName(schema.packageName, parentName)
        typeBuilder.superclass(typeName)

        parent?.fields?.forEach { parentField ->
            val childField = schema.fields.firstOrNull { it.generatedName == parentField.generatedName }
            typeBuilder.addSuperclassConstructorParameter(
                "%N",
                childField?.storageName(renderOverriddenInCtorOnly) ?: parentField.generatedName,
            )
        }
    }
}

internal fun addImplementedInterfaces(
    typeBuilder: TypeSpec.Builder,
    schema: ModelDO,
    interfaces: List<String>,
    byName: Map<String, ModelDO>,
) {
    interfaces.forEach { ifaceName ->
        val iface = byName[ifaceName]
        val typeName = iface?.className() ?: ClassName(schema.packageName, ifaceName)
        typeBuilder.addSuperinterface(typeName)
    }
}
