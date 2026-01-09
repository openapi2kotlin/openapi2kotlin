package dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.model.helpers

import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.EnumValue
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.FieldTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.ListTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.ModelDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.ModelShapeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.RefTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.TrivialTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.raw.RawSchemaDO

internal fun List<ModelDO>.handleModelShape() {
    val byName = associateBy { it.rawSchema.originalName }

    // Discriminator mappings reference concrete subtypes even when they are not used in paths or properties.
    val discriminatorChildNames: Set<String> = buildSet {
        for (m in this@handleModelShape) {
            val mapping = m.rawSchema.discriminatorMapping
            if (mapping.isEmpty()) continue
            mapping.forEach { (k, v) ->
                add(k)
                add(v.substringAfterLast('/'))
            }
        }
    }

    // Materialize discriminator-child usage on ModelDO (core model, not raw).
    forEach { component ->
        component.usedAsDiscriminatorChild = component.rawSchema.originalName in discriminatorChildNames
    }

    forEach { component ->
        if (component.rawSchema.isArraySchema && component.rawSchema.arrayItemType != null) {
            component.modelShape = ModelShapeDO.TypeAlias(
                target = ListTypeDO(
                    elementType = component.rawSchema.arrayItemType.toFieldTypeDO(),
                    nullable = false,
                )
            )
            return@forEach
        }

        if (component.rawSchema.enumValues.isNotEmpty()) {
            val enumValues = component.rawSchema.enumValues.map { original ->
                EnumValue(
                    originalValue = original,
                    generatedValue = original.toEnumConstName(),
                )
            }

            component.modelShape = ModelShapeDO.EnumClass(values = enumValues)
            return@forEach
        }

        val hasOneOf = component.rawSchema.oneOfChildren.isNotEmpty()

        val isAllOfBase = component.allOfChildren.isNotEmpty()

        // Leaf schemas often have discriminator mapping only to themselves. That must NOT force OpenClass.
        val hasDiscriminator = component.rawSchema.discriminatorPropertyName != null
        val isInstantiableSelf = hasDiscriminator && component.rawSchema.isDiscriminatorSelfMapped

        // This is the special case: the schema is used as an allOf base AND is instantiable as itself.
        val isConcreteAllOfBase = isAllOfBase && isInstantiableSelf

        val isUsedSomewhere =
            component.rawSchema.usedInPaths ||
                    component.rawSchema.usedAsProperty ||
                    component.usedAsDiscriminatorChild

        val isAbstractAllOfBase =
            isAllOfBase &&
                    !isUsedSomewhere &&
                    !isConcreteAllOfBase

        component.modelShape = when {
            // concrete base => must be instantiable
            isConcreteAllOfBase ->
                ModelShapeDO.OpenClass(extend = null, implements = emptyList())

            // union base or abstract base => sealed interface
            hasOneOf || isAbstractAllOfBase ->
                ModelShapeDO.SealedInterface(extends = emptyList())

            // leaf => data class
            !isAllOfBase ->
                ModelShapeDO.DataClass(
                    extend = null,
                    implements = emptyList(),
                )

            // base but not abstract and not concrete => open class
            else ->
                ModelShapeDO.OpenClass(
                    extend = null,
                    implements = emptyList(),
                )
        }
    }

    forEach { component ->
        when (component.modelShape) {
            is ModelShapeDO.EnumClass,
            is ModelShapeDO.TypeAlias -> return@forEach
            else -> {}
        }

        val allOfParents = component.rawSchema.allOfParents
        var parentClass: String? = null
        val parentInterfaces = mutableListOf<String>()

        allOfParents.forEach { parentName ->
            val parent = byName[parentName] ?: return@forEach
            when (parent.modelShape) {
                is ModelShapeDO.SealedInterface -> if (!parentInterfaces.contains(parentName)) parentInterfaces += parentName
                is ModelShapeDO.OpenClass,
                is ModelShapeDO.DataClass -> if (parentClass == null) parentClass = parentName
                is ModelShapeDO.EnumClass,
                is ModelShapeDO.TypeAlias,
                is ModelShapeDO.Undecided -> {}
            }
        }

        component.parentOneOf?.let { parentName ->
            if (!parentInterfaces.contains(parentName)) parentInterfaces += parentName
        }

        component.modelShape = when (val shape = component.modelShape) {
            is ModelShapeDO.SealedInterface ->
                shape.copy(extends = parentInterfaces)

            is ModelShapeDO.DataClass ->
                shape.copy(extend = parentClass, implements = parentInterfaces)

            is ModelShapeDO.OpenClass ->
                shape.copy(extend = parentClass, implements = parentInterfaces)

            is ModelShapeDO.EnumClass,
            is ModelShapeDO.TypeAlias,
            is ModelShapeDO.Undecided ->
                shape
        }
    }
}

private fun RawSchemaDO.RawFieldTypeDO.toFieldTypeDO(): FieldTypeDO = when (this) {
    is RawSchemaDO.RawRefTypeDO ->
        RefTypeDO(schemaName = schemaName, nullable = nullable)

    is RawSchemaDO.RawArrayTypeDO ->
        ListTypeDO(elementType = elementType.toFieldTypeDO(), nullable = nullable)

    is RawSchemaDO.RawPrimitiveTypeDO -> {
        val trivial = when (type) {
            RawSchemaDO.RawPrimitiveTypeDO.Type.STRING -> TrivialTypeDO.Kind.STRING
            RawSchemaDO.RawPrimitiveTypeDO.Type.BOOLEAN -> TrivialTypeDO.Kind.BOOLEAN
            RawSchemaDO.RawPrimitiveTypeDO.Type.INTEGER -> {
                if (format == "int64") TrivialTypeDO.Kind.LONG else TrivialTypeDO.Kind.INT
            }
            RawSchemaDO.RawPrimitiveTypeDO.Type.NUMBER -> TrivialTypeDO.Kind.DOUBLE
            RawSchemaDO.RawPrimitiveTypeDO.Type.OBJECT -> TrivialTypeDO.Kind.ANY
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

        // Any non-letter/digit is a separator => boundary
        if (!c.isLetterOrDigit()) {
            flush()
            continue
        }

        val prev = s.getOrNull(i - 1)
        val next = s.getOrNull(i + 1)

        val prevIsLower = prev?.isLowerCase() == true
        val prevIsUpper = prev?.isUpperCase() == true
        val prevIsDigit = prev?.isDigit() == true
        val prevIsLetter = prev?.isLetter() == true

        val cIsUpper = c.isUpperCase()
        val cIsLower = c.isLowerCase()
        val cIsDigit = c.isDigit()

        val nextIsLower = next?.isLowerCase() == true

        val boundary =
            (prev != null) && (
                    // camelCase boundary: inProgress => in + Progress
                    (prevIsLower && cIsUpper) ||
                            // acronym boundary: HTTPServer => HTTP + Server (split before S)
                            (prevIsUpper && cIsUpper && nextIsLower) ||
                            // digit boundaries
                            (prevIsLetter && cIsDigit) ||
                            (prevIsDigit && (cIsUpper || cIsLower))
                    )

        if (boundary) flush()
        current.append(c)
    }
    flush()

    var name = tokens
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

private val KOTLIN_KEYWORDS = setOf(
    "AS", "BREAK", "CLASS", "CONTINUE", "DO", "ELSE", "FALSE", "FOR", "FUN", "IF",
    "IN", "INTERFACE", "IS", "NULL", "OBJECT", "PACKAGE", "RETURN", "SUPER",
    "THIS", "THROW", "TRUE", "TRY", "TYPEALIAS", "VAL", "VAR", "WHEN", "WHILE"
)
