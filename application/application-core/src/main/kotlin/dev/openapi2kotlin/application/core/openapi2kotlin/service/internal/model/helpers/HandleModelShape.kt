package dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.model.helpers

import dev.openapi2kotlin.application.core.openapi2kotlin.domain.model.EnumValueDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.model.FieldTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.model.ListTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.model.ModelDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.model.ModelShapeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.model.RefTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.model.TrivialTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.raw.RawSchemaDO
import dev.openapi2kotlin.application.usecase.openapi2kotlin.OpenApi2KotlinUseCase

internal fun List<ModelDO>.handleModelShape(cfg: OpenApi2KotlinUseCase.ModelConfig) {
    val byName = associateBy { it.rawSchema.originalName }
    markDiscriminatorChildren()
    forEach { component -> component.modelShape = component.resolveInitialShape(cfg) }
    forEach { component -> component.modelShape = component.resolveInheritedShape(byName) }
}

private fun RawSchemaDO.RawFieldTypeDO.toFieldTypeDO(): FieldTypeDO =
    when (this) {
        is RawSchemaDO.RawRefTypeDO -> {
            RefTypeDO(schemaName = schemaName, nullable = nullable)
        }

        is RawSchemaDO.RawArrayTypeDO -> {
            ListTypeDO(elementType = elementType.toFieldTypeDO(), nullable = nullable)
        }

        is RawSchemaDO.RawPrimitiveTypeDO -> {
            val trivial =
                when (type) {
                    RawSchemaDO.RawPrimitiveTypeDO.Type.STRING -> {
                        TrivialTypeDO.Kind.STRING
                    }

                    RawSchemaDO.RawPrimitiveTypeDO.Type.BOOLEAN -> {
                        TrivialTypeDO.Kind.BOOLEAN
                    }

                    RawSchemaDO.RawPrimitiveTypeDO.Type.INTEGER -> {
                        if (format == "int64") TrivialTypeDO.Kind.LONG else TrivialTypeDO.Kind.INT
                    }

                    RawSchemaDO.RawPrimitiveTypeDO.Type.NUMBER -> {
                        TrivialTypeDO.Kind.DOUBLE
                    }

                    RawSchemaDO.RawPrimitiveTypeDO.Type.OBJECT -> {
                        TrivialTypeDO.Kind.ANY
                    }
                }
            TrivialTypeDO(kind = trivial, nullable = nullable)
        }
    }

private fun String.toEnumConstName(): String {
    val s = trim()
    if (s.isEmpty()) return "UNDEFINED"

    val tokens = mutableListOf<String>()
    val current = StringBuilder()

    fun flush() {
        if (current.isNotEmpty()) {
            tokens += current.toString()
            current.setLength(0)
        }
    }

    for (i in s.indices) {
        val c = s[i]
        if (!c.isLetterOrDigit()) {
            flush()
            continue
        }
        if (s.hasIdentifierBoundaryAt(i)) flush()
        current.append(c)
    }
    flush()

    var name =
        tokens
            .asSequence()
            .filter { it.isNotBlank() }
            .joinToString("_") { it.uppercase() }
            .replace(Regex("_+"), "_")
            .trim('_')

    if (name.isEmpty()) name = "UNDEFINED"

    // Kotlin identifier cannot start with digit
    if (name.first().isDigit()) name = "_$name"

    // Avoid Kotlin keywords as enum entries (conservative)
    if (name in KOTLIN_KEYWORDS) name = "${name}_"

    return name
}

private fun List<ModelDO>.markDiscriminatorChildren() {
    val discriminatorChildNames =
        buildSet {
            this@markDiscriminatorChildren.forEach { model ->
                model.rawSchema.discriminatorMapping.forEach { (discriminatorName, reference) ->
                    add(discriminatorName)
                    add(reference.substringAfterLast('/'))
                }
            }
        }

    forEach { component ->
        component.usedAsDiscriminatorChild = component.rawSchema.originalName in discriminatorChildNames
    }
}

private fun ModelDO.resolveInitialShape(cfg: OpenApi2KotlinUseCase.ModelConfig): ModelShapeDO {
    val immediateShape = toArrayAliasShape() ?: toEnumShape()
    if (immediateShape != null) return immediateShape

    val hasOneOf = rawSchema.oneOfChildren.isNotEmpty()
    val isAllOfBase = allOfChildren.isNotEmpty()
    val useInterfaceForAllOfBase =
        cfg.serialization == OpenApi2KotlinUseCase.ModelConfig.Serialization.KOTLINX && isAllOfBase
    val isConcreteAllOfBase = isAllOfBase && isInstantiableSelf()
    val isAbstractAllOfBase = isAllOfBase && !isUsedSomewhere() && !isConcreteAllOfBase

    return when {
        useInterfaceForAllOfBase -> ModelShapeDO.SealedInterface(extends = emptyList())
        isConcreteAllOfBase -> ModelShapeDO.OpenClass(extend = null, implements = emptyList())
        hasOneOf || isAbstractAllOfBase -> ModelShapeDO.SealedInterface(extends = emptyList())
        !isAllOfBase -> ModelShapeDO.DataClass(extend = null, implements = emptyList())
        else -> ModelShapeDO.OpenClass(extend = null, implements = emptyList())
    }
}

private fun ModelDO.toArrayAliasShape(): ModelShapeDO.TypeAlias? {
    val itemType = rawSchema.arrayItemType
    return if (rawSchema.isArraySchema && itemType != null) {
        ModelShapeDO.TypeAlias(
            target =
                ListTypeDO(
                    elementType = itemType.toFieldTypeDO(),
                    nullable = false,
                ),
        )
    } else {
        null
    }
}

private fun ModelDO.toEnumShape(): ModelShapeDO.EnumClass? {
    if (rawSchema.enumValues.isEmpty()) return null
    return ModelShapeDO.EnumClass(
        values =
            rawSchema.enumValues.map { original ->
                EnumValueDO(
                    originalValue = original,
                    generatedValue = original.toEnumConstName(),
                )
            },
    )
}

private fun ModelDO.isInstantiableSelf(): Boolean =
    rawSchema.discriminatorPropertyName != null && rawSchema.isDiscriminatorSelfMapped

private fun ModelDO.isUsedSomewhere(): Boolean = rawSchema.usedInPaths || usedAsDiscriminatorChild

private fun ModelDO.resolveInheritedShape(byName: Map<String, ModelDO>): ModelShapeDO {
    if (modelShape is ModelShapeDO.EnumClass || modelShape is ModelShapeDO.TypeAlias) return modelShape

    val parentClass = rawSchema.allOfParents.firstResolvableParentClass(byName)
    val parentInterfaces =
        buildList {
            rawSchema.allOfParents.forEach { parentName ->
                if (byName[parentName]?.modelShape is ModelShapeDO.SealedInterface && parentName !in this) {
                    add(parentName)
                }
            }
            parentOneOf.forEach { parentName ->
                if (parentName !in this) add(parentName)
            }
        }

    return when (val shape = modelShape) {
        is ModelShapeDO.SealedInterface -> shape.copy(extends = parentInterfaces)

        is ModelShapeDO.DataClass -> shape.copy(extend = parentClass, implements = parentInterfaces)

        is ModelShapeDO.EmptyClass -> shape.copy(extend = parentClass, implements = parentInterfaces)

        is ModelShapeDO.OpenClass -> shape.copy(extend = parentClass, implements = parentInterfaces)

        is ModelShapeDO.EnumClass,
        is ModelShapeDO.TypeAlias,
        is ModelShapeDO.Undecided,
        -> shape
    }
}

private val KOTLIN_KEYWORDS =
    setOf(
        "AS",
        "BREAK",
        "CLASS",
        "CONTINUE",
        "DO",
        "ELSE",
        "FALSE",
        "FOR",
        "FUN",
        "IF",
        "IN",
        "INTERFACE",
        "IS",
        "NULL",
        "OBJECT",
        "PACKAGE",
        "RETURN",
        "SUPER",
        "THIS",
        "THROW",
        "TRUE",
        "TRY",
        "TYPEALIAS",
        "VAL",
        "VAR",
        "WHEN",
        "WHILE",
    )
