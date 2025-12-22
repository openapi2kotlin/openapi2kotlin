package dev.openapi2kotlin.adapter.generatemodel.internal

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeAliasSpec
import com.squareup.kotlinpoet.TypeSpec
import dev.openapi2kotlin.adapter.generatemodel.internal.helpers.applyModelAnnotations
import dev.openapi2kotlin.adapter.generatemodel.internal.helpers.applyPropertyAnnotations
import dev.openapi2kotlin.adapter.generatemodel.internal.helpers.className
import dev.openapi2kotlin.adapter.generatemodel.internal.helpers.toParamSpec
import dev.openapi2kotlin.adapter.generatemodel.internal.helpers.typeName
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.ModelDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.ModelShapeDO
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

    models.forEach { model ->
        val fileSpec: FileSpec = when (val shape = model.modelShape) {
            is ModelShapeDO.EnumClass ->
                buildEnumFile(model, shape)

            is ModelShapeDO.SealedInterface ->
                buildSealedInterfaceFile(model, shape, byName)

            is ModelShapeDO.DataClass ->
                buildDataClassFile(model, shape, byName)

            is ModelShapeDO.OpenClass ->
                buildOpenClassFile(model, shape, byName)

            is ModelShapeDO.TypeAlias ->
                buildTypeAliasFile(model, shape, byName)

            is ModelShapeDO.Undecided -> {
                log.warn { "Shape for ${model.generatedName} is still Undecided, generating simple data class." }
                buildDataClassFile(
                    model,
                    ModelShapeDO.DataClass(
                        extend = null,
                        implements = emptyList(),
                    ),
                    byName,
                )
            }
        }

        fileSpec.writeTo(outputDir)
    }

    // strip 'public ' everywhere – you don't want explicit visibility
    outputDir
        .walkTopDown()
        .filter { it.isFile && it.extension == "kt" }
        .forEach { file ->
            val text = file.readText()
            file.writeText(text
                // KotlinPoet add redundant public modifiers by default
                .replace("public ", "")
                // KotlinPoet escapes this package segment; we prefer the normal form.
                .replace("com.fasterxml.jackson.`annotation`.", "com.fasterxml.jackson.annotation.")
            )
        }
}

/* ---------- ENUM CLASS ---------- */

private fun buildEnumFile(
    schema: ModelDO,
    shape: ModelShapeDO.EnumClass,
): FileSpec {
    val typeBuilder = TypeSpec.enumBuilder(schema.generatedName)
        .applyModelAnnotations(schema)

    val ctor = FunSpec.constructorBuilder()
        .addParameter("value", String::class)
        .build()
    typeBuilder.primaryConstructor(ctor)

    typeBuilder.addProperty(
        PropertySpec.builder("value", String::class)
            .initializer("value")
            .build()
    )

    shape.values.forEach { rawValue ->
        val constName = rawValue
            .uppercase()
            .replace(' ', '_')
            .replace('-', '_')

        val enumConst = TypeSpec.anonymousClassBuilder()
            .addSuperclassConstructorParameter("%S", rawValue)
            .build()

        typeBuilder.addEnumConstant(constName, enumConst)
    }

    val fromValueFun = FunSpec.builder("fromValue")
        .returns(schema.className())
        .addParameter("v", String::class)
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
): FileSpec {
    val typeBuilder = TypeSpec.interfaceBuilder(schema.generatedName)
        .addModifiers(KModifier.SEALED)
        .applyModelAnnotations(schema)

    shape.extends.forEach { parentName ->
        val parent = byName[parentName]
        val typeName = parent?.className() ?: ClassName(schema.packageName, parentName)
        typeBuilder.addSuperinterface(typeName)
    }

    schema.fields.forEach { field ->
        val propBuilder = PropertySpec.builder(
            field.generatedName,
            field.type.typeName(schema, byName),
        )

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
): FileSpec {
    val ctor = FunSpec.constructorBuilder().apply {
        schema.fields.forEach { field ->
            addParameter(field.toParamSpec(schema, byName))
        }
    }.build()

    val typeBuilder = TypeSpec.classBuilder(schema.generatedName)
        .addModifiers(KModifier.DATA)
        .primaryConstructor(ctor)
        .applyModelAnnotations(schema)

    val hasChildren = schema.allOfChildren.isNotEmpty()

    schema.fields.forEach { field ->
        val propBuilder = PropertySpec.builder(
            field.generatedName,
            field.type.typeName(schema, byName),
        ).initializer(field.generatedName)

        field.applyPropertyAnnotations(propBuilder)

        if (field.overridden) {
            propBuilder.addModifiers(KModifier.OVERRIDE)
        } else if (hasChildren) {
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
): FileSpec {
    val ctor = FunSpec.constructorBuilder().apply {
        schema.fields.forEach { field ->
            addParameter(field.toParamSpec(schema, byName))
        }
    }.build()

    val typeBuilder = TypeSpec.classBuilder(schema.generatedName)
        .addModifiers(KModifier.OPEN)
        .primaryConstructor(ctor)
        .applyModelAnnotations(schema)

    val hasChildren = schema.allOfChildren.isNotEmpty()

    schema.fields.forEach { field ->
        val propBuilder = PropertySpec.builder(
            field.generatedName,
            field.type.typeName(schema, byName),
        ).initializer(field.generatedName)

        field.applyPropertyAnnotations(propBuilder)

        if (field.overridden) {
            propBuilder.addModifiers(KModifier.OVERRIDE)
        } else if (hasChildren) {
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
    byName: Map<String, ModelDO>,
): FileSpec {
    val targetTypeName = shape.target.typeName(schema, byName)

    return FileSpec.builder(schema.packageName, schema.generatedName)
        .addTypeAlias(TypeAliasSpec.builder(schema.generatedName, targetTypeName).build())
        .build()
}
