package dev.openapi2kotlin.adapter.generatemodel.internal

import com.squareup.kotlinpoet.*
import dev.openapi2kotlin.adapter.generatemodel.internal.helpers.applyAnnotations
import dev.openapi2kotlin.adapter.generatemodel.internal.helpers.applyModelAnnotations
import dev.openapi2kotlin.adapter.generatemodel.internal.helpers.applyPropertyAnnotations
import dev.openapi2kotlin.adapter.generatemodel.internal.helpers.toParamSpec
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.ModelDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.ModelShapeDO
import dev.openapi2kotlin.tools.generatortools.TypeNameContext
import dev.openapi2kotlin.tools.generatortools.postProcess
import dev.openapi2kotlin.tools.generatortools.toTypeName
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.file.Path

private val log = KotlinLogging.logger {}

/**
 * Final pass – generate Kotlin files.
 *
 * No decision logic here – everything must be pre-calculated in ModelDO.
 */
fun generate(
    models: List<ModelDO>,
    outputDirPath: Path,
) {
    val outputDir = outputDirPath.toFile()
    val byName = models.associateBy { it.rawSchema.originalName }

    val ctxByPackageName: Map<String, TypeNameContext> =
        models
            .map { it.packageName }
            .distinct()
            .associateWith { pkg ->
                TypeNameContext(
                    modelPackageName = pkg,
                    bySchemaName = byName,
                )
            }

    models.forEach { model ->
        val fileSpec: FileSpec = when (val shape = model.modelShape) {
            is ModelShapeDO.EnumClass ->
                buildEnumFile(model, shape)

            is ModelShapeDO.SealedInterface ->
                buildSealedInterfaceFile(model, shape, byName, ctxByPackageName)

            is ModelShapeDO.DataClass ->
                buildDataClassFile(model, shape, byName, ctxByPackageName)

            is ModelShapeDO.OpenClass ->
                buildOpenClassFile(model, shape, byName, ctxByPackageName)

            is ModelShapeDO.TypeAlias ->
                buildTypeAliasFile(model, shape, ctxByPackageName)

            is ModelShapeDO.Undecided -> {
                log.warn { "Shape for ${model.generatedName} is still Undecided, generating simple data class." }
                buildDataClassFile(
                    model,
                    ModelShapeDO.DataClass(
                        extend = null,
                        implements = emptyList(),
                    ),
                    byName,
                    ctxByPackageName,
                )
            }
        }

        fileSpec.writeTo(outputDir)
    }

    outputDir.postProcess()
}

/* ---------- ENUM CLASS ---------- */

private fun buildEnumFile(
    schema: ModelDO,
    shape: ModelShapeDO.EnumClass,
): FileSpec {
    val typeBuilder = TypeSpec.enumBuilder(schema.generatedName)
        .applyModelAnnotations(schema)

    schema.kdoc?.takeIf { it.isNotBlank() }?.let { doc ->
        typeBuilder.addKdoc("%L\n", doc.trim())
    }

    val ctor = FunSpec.constructorBuilder()
        .addParameter("value", String::class)
        .build()
    typeBuilder.primaryConstructor(ctor)

    typeBuilder.addProperty(
        PropertySpec.builder("value", String::class)
            .initializer("value")
            .applyAnnotations(schema.enumValueAnnotations)
            .build()
    )

    shape.values.forEach { ev ->
        val enumConst = TypeSpec.anonymousClassBuilder()
            .addSuperclassConstructorParameter("%S", ev.originalValue)
            .build()

        typeBuilder.addEnumConstant(ev.generatedValue, enumConst)
    }

    val fromValueFun = FunSpec.builder("fromValue")
        .returns(schema.className())
        .addParameter("v", String::class)
        .applyAnnotations(schema.enumFromValueAnnotations)
        .addCode(
            """
            return entries.firstOrNull { it.value == v }
                ?: throw IllegalArgumentException("Unexpected value for ${schema.generatedName}: '${'$'}v'")
            """.trimIndent()
        )
        .build()

    val companion = TypeSpec.companionObjectBuilder()
        .addFunction(fromValueFun)
        .build()

    typeBuilder.addType(companion)

    return FileSpec
        .builder(schema.packageName, schema.generatedName)
        .addType(typeBuilder.build())
        .build()
}

/* ---------- SEALED INTERFACE ---------- */

private fun buildSealedInterfaceFile(
    schema: ModelDO,
    shape: ModelShapeDO.SealedInterface,
    byName: Map<String, ModelDO>,
    ctxByPackageName: Map<String, TypeNameContext>,
): FileSpec {
    val ctx = ctxByPackageName.getValue(schema.packageName)

    val typeBuilder = TypeSpec.interfaceBuilder(schema.generatedName)
        .addModifiers(KModifier.SEALED)
        .applyModelAnnotations(schema)

    schema.kdoc?.takeIf { it.isNotBlank() }?.let { doc ->
        typeBuilder.addKdoc("%L\n", doc.trim())
    }

    shape.extends.forEach { parentName ->
        val parent = byName[parentName]
        val typeName = parent?.className() ?: ClassName(schema.packageName, parentName)
        typeBuilder.addSuperinterface(typeName)
    }

    schema.fields.forEach { field ->
        val propBuilder = PropertySpec.builder(
            field.generatedName,
            field.type.toTypeName(ctx),
        )

        field.kdoc?.takeIf { it.isNotBlank() }?.let { doc ->
            propBuilder.addKdoc("%L\n", doc.trim())
        }

        field.applyPropertyAnnotations(propBuilder)

        if (field.overridden) {
            propBuilder.addModifiers(KModifier.OVERRIDE)
        }

        typeBuilder.addProperty(propBuilder.build())
    }

    return FileSpec
        .builder(schema.packageName, schema.generatedName)
        .addType(typeBuilder.build())
        .build()
}

/* ---------- DATA CLASS ---------- */

private fun buildDataClassFile(
    schema: ModelDO,
    shape: ModelShapeDO.DataClass,
    byName: Map<String, ModelDO>,
    ctxByPackageName: Map<String, TypeNameContext>,
): FileSpec {
    val ctx = ctxByPackageName.getValue(schema.packageName)

    val ctor = FunSpec.constructorBuilder().apply {
        schema.fields.forEach { field ->
            addParameter(field.toParamSpec(ctx))
        }
    }.build()

    val typeBuilder = TypeSpec.classBuilder(schema.generatedName)
        .addModifiers(KModifier.DATA)
        .primaryConstructor(ctor)
        .applyModelAnnotations(schema)

    schema.kdoc?.takeIf { it.isNotBlank() }?.let { doc ->
        typeBuilder.addKdoc("%L\n", doc.trim())
    }

    val shouldOpenProps = schema.allOfChildren.isNotEmpty()

    schema.fields.forEach { field ->
        val propBuilder = PropertySpec.builder(
            field.generatedName,
            field.type.toTypeName(ctx),
        ).initializer(field.generatedName)

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

    shape.extend?.let { parentName ->
        val parent = byName[parentName]
        val typeName = parent?.className() ?: ClassName(schema.packageName, parentName)
        typeBuilder.superclass(typeName)

        parent?.fields?.forEach { parentField ->
            typeBuilder.addSuperclassConstructorParameter(parentField.generatedName)
        }
    }

    shape.implements.forEach { ifaceName ->
        val iface = byName[ifaceName]
        val typeName = iface?.className() ?: ClassName(schema.packageName, ifaceName)
        typeBuilder.addSuperinterface(typeName)
    }

    return FileSpec
        .builder(schema.packageName, schema.generatedName)
        .addType(typeBuilder.build())
        .build()
}

/* ---------- OPEN CLASS ---------- */

private fun buildOpenClassFile(
    schema: ModelDO,
    shape: ModelShapeDO.OpenClass,
    byName: Map<String, ModelDO>,
    ctxByPackageName: Map<String, TypeNameContext>,
): FileSpec {
    val ctx = ctxByPackageName.getValue(schema.packageName)

    val ctor = FunSpec.constructorBuilder().apply {
        schema.fields.forEach { field ->
            addParameter(field.toParamSpec(ctx))
        }
    }.build()

    val typeBuilder = TypeSpec.classBuilder(schema.generatedName)
        .addModifiers(KModifier.OPEN)
        .primaryConstructor(ctor)
        .applyModelAnnotations(schema)

    schema.kdoc?.takeIf { it.isNotBlank() }?.let { doc ->
        typeBuilder.addKdoc("%L\n", doc.trim())
    }

    // Open classes exist because they are used as bases.
    // Their properties must be open so children can override cleanly.
    val shouldOpenProps = true

    schema.fields.forEach { field ->
        val propBuilder = PropertySpec.builder(
            field.generatedName,
            field.type.toTypeName(ctx),
        ).initializer(field.generatedName)

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

    shape.extend?.let { parentName ->
        val parent = byName[parentName]
        val typeName = parent?.className() ?: ClassName(schema.packageName, parentName)
        typeBuilder.superclass(typeName)

        parent?.fields?.forEach { parentField ->
            typeBuilder.addSuperclassConstructorParameter(parentField.generatedName)
        }
    }

    shape.implements.forEach { ifaceName ->
        val iface = byName[ifaceName]
        val typeName = iface?.className() ?: ClassName(schema.packageName, ifaceName)
        typeBuilder.addSuperinterface(typeName)
    }

    return FileSpec
        .builder(schema.packageName, schema.generatedName)
        .addType(typeBuilder.build())
        .build()
}

/* ---------- TYPEALIAS ---------- */

private fun buildTypeAliasFile(
    schema: ModelDO,
    shape: ModelShapeDO.TypeAlias,
    ctxByPackageName: Map<String, TypeNameContext>,
): FileSpec {
    val ctx = ctxByPackageName.getValue(schema.packageName)
    val targetTypeName = shape.target.toTypeName(ctx)

    val fileBuilder = FileSpec.builder(schema.packageName, schema.generatedName)

    schema.kdoc?.takeIf { it.isNotBlank() }?.let { doc ->
        fileBuilder.addFileComment(doc.trim())
    }

    return fileBuilder
        .addTypeAlias(TypeAliasSpec.builder(schema.generatedName, targetTypeName).build())
        .build()
}

private fun ModelDO.className(): ClassName =
    ClassName(packageName, generatedName)
