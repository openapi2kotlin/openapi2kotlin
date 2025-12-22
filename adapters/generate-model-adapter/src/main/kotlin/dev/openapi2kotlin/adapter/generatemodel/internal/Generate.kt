package dev.openapi2kotlin.adapter.generatemodel.internal

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.ModelShapeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.ListTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.PrimitiveTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.RefTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.ModelDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.FieldTypeDO
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.file.Path

private val log = KotlinLogging.logger {}

/**
 * Final pass – generate Kotlin files.
 *
 * No decision logic here – everything must be pre-calculated in SchemaComponent.
 */
fun generate(
    models: List<ModelDO>,
    outputDirPath: Path,
) {
    val outputDir = outputDirPath.toFile()
    val byName =models.associateBy { it.rawSchema.originalName }

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
            file.writeText(text.replace("public ", ""))
        }
}

/* ---------- ENUM CLASS ---------- */
private fun buildEnumFile(
    schema: ModelDO,
    shape: ModelShapeDO.EnumClass,
): FileSpec {
    val typeBuilder = TypeSpec.Companion.enumBuilder(schema.generatedName)

    // primary constructor(val value: String)
    val ctor = FunSpec.constructorBuilder()
        .addParameter("value", String::class)
        .build()
    typeBuilder.primaryConstructor(ctor)

    typeBuilder.addProperty(
        PropertySpec.builder("value", String::class)
            .initializer("value")
            .build()
    )

    // enum constants
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

    // companion object with fromValue
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
    val typeBuilder = TypeSpec.Companion.interfaceBuilder(schema.generatedName)
        .addModifiers(KModifier.SEALED)

    // superinterfaces
    shape.extends.forEach { parentName ->
        val parent = byName[parentName]
        val typeName = parent?.className() ?: ClassName(schema.packageName, parentName)
        typeBuilder.addSuperinterface(typeName)
    }

    // properties
    schema.fields.forEach { field ->
        val propBuilder = PropertySpec.builder(
            field.generatedName,
            field.type.typeName(schema, byName),
        )
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
            addParameter(
                ParameterSpec.builder(
                    field.generatedName,
                    field.type.typeName(schema, byName),
                ).build()
            )
        }
    }.build()

    val typeBuilder = TypeSpec.Companion.classBuilder(schema.generatedName)
        .addModifiers(KModifier.DATA)
        .primaryConstructor(ctor)

    val hasChildren = schema.allOfChildren.isNotEmpty()

    schema.fields.forEach { field ->
        val propBuilder = PropertySpec.builder(
            field.generatedName,
            field.type.typeName(schema, byName),
        ).initializer(field.generatedName)

        if (field.overridden) {
            // override only; override is implicitly open
            propBuilder.addModifiers(KModifier.OVERRIDE)
        } else if (hasChildren) {
            // declared here, and subclasses exist → open
            propBuilder.addModifiers(KModifier.OPEN)
        }

        typeBuilder.addProperty(propBuilder.build())
    }

    // superclass + super ctor params
    shape.extend?.let { parentName ->
        val parent = byName[parentName]
        val typeName = parent?.className() ?: ClassName(schema.packageName, parentName)
        typeBuilder.superclass(typeName)

        parent?.fields?.forEach { parentField ->
            typeBuilder.addSuperclassConstructorParameter(parentField.generatedName)
        }
    }

    // interfaces
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
            val paramBuilder = ParameterSpec.builder(
                field.generatedName,
                field.type.typeName(schema, byName),
            )
            if (field.type.nullable) {
                paramBuilder.defaultValue("null")
            }
            addParameter(paramBuilder.build())
        }
    }.build()

    val typeBuilder = TypeSpec.Companion.classBuilder(schema.generatedName)
        .addModifiers(KModifier.OPEN)
        .primaryConstructor(ctor)

    val hasChildren = schema.allOfChildren.isNotEmpty()

    schema.fields.forEach { field ->
        val propBuilder = PropertySpec.builder(
            field.generatedName,
            field.type.typeName(schema, byName),
        ).initializer(field.generatedName)

        if (field.overridden) {
            // override only; implicitly open further
            propBuilder.addModifiers(KModifier.OVERRIDE)
        } else if (hasChildren) {
            // base property that subclasses can override
            propBuilder.addModifiers(KModifier.OPEN)
        }

        typeBuilder.addProperty(propBuilder.build())
    }

    // superclass + super ctor params
    shape.extend?.let { parentName ->
        val parent = byName[parentName]
        val typeName = parent?.className() ?: ClassName(schema.packageName, parentName)
        typeBuilder.superclass(typeName)

        parent?.fields?.forEach { parentField ->
            typeBuilder.addSuperclassConstructorParameter(parentField.generatedName)
        }
    }

    // interfaces
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
        .addTypeAlias(
            TypeAliasSpec.builder(schema.generatedName, targetTypeName).build()
        )
        .build()
}

/* ---------- KotlinPoet helpers ---------- */

private val STRING = ClassName("kotlin", "String")
private val INT = ClassName("kotlin", "Int")
private val LONG = ClassName("kotlin", "Long")
private val DOUBLE = ClassName("kotlin", "Double")
private val BOOLEAN = ClassName("kotlin", "Boolean")
private val ANY = ClassName("kotlin", "Any")
private val LIST = ClassName("kotlin.collections", "List")

private fun ModelDO.className(): ClassName =
    ClassName(packageName, generatedName)

private fun PrimitiveTypeDO.PrimitiveTypeNameDO.typeName(): ClassName = when (this) {
    PrimitiveTypeDO.PrimitiveTypeNameDO.STRING -> STRING
    PrimitiveTypeDO.PrimitiveTypeNameDO.INT -> INT
    PrimitiveTypeDO.PrimitiveTypeNameDO.LONG -> LONG
    PrimitiveTypeDO.PrimitiveTypeNameDO.DOUBLE -> DOUBLE
    PrimitiveTypeDO.PrimitiveTypeNameDO.BOOLEAN -> BOOLEAN
    PrimitiveTypeDO.PrimitiveTypeNameDO.ANY -> ANY
}

private fun FieldTypeDO.typeName(
    schema: ModelDO,
    byName: Map<String, ModelDO>,
): TypeName = when (this) {
    is PrimitiveTypeDO -> name.typeName().copy(nullable = nullable)

    is RefTypeDO -> {
        val target = byName[schemaName]
        val cls = target?.className() ?: ClassName(schema.packageName, schemaName)
        cls.copy(nullable = nullable)
    }

    is ListTypeDO -> {
        val elementTypeName = elementType.typeName(schema, byName)
        LIST.parameterizedBy(elementTypeName).copy(nullable = nullable)
    }
}
