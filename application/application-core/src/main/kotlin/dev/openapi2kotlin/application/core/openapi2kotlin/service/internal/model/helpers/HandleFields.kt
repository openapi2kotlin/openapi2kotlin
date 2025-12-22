package dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.model.helpers

import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.FieldDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.FieldTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.ListTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.ModelDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.ModelShapeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.RefTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.TrivialTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.raw.RawSchemaDO
import dev.openapi2kotlin.application.usecase.openapi2kotlin.OpenApi2KotlinUseCase

internal fun List<ModelDO>.handleFields(
    cfg: OpenApi2KotlinUseCase.ModelConfig.MappingConfig,
) {
    val byName: Map<String, ModelDO> = associateBy { it.rawSchema.originalName }

    forEach { component ->
        val parentPropSchemas = mutableMapOf<String, PropInfo>()
        component.rawSchema.allOfParents.forEach { parentName ->
            collectAllPropertiesFromSchema(
                schemaName = parentName,
                schemas = byName,
                into = parentPropSchemas,
                visited = mutableSetOf(),
            )
        }

        val fields = mutableListOf<FieldDO>()

        parentPropSchemas.forEach { (propName, info) ->
            fields += FieldDO(
                originalName = propName,
                generatedName = propName.toKotlinName(),
                overridden = true,
                type = info.type,
                required = info.required,
                defaultValueCode = info.defaultValueCode,
            )
        }

        val ownPropSchemas = component.rawSchema.ownProperties
            .mapValues { (_, prop) ->
                PropInfo(
                    type = prop.type.toFinalType(cfg, owner = component, bySchemaName = byName),
                    required = prop.required,
                    defaultValueCode = prop.defaultValue?.let { renderDefault(prop.type, cfg, it) },
                )
            }
            .toMutableMap()

        ownPropSchemas.forEach { (propName, info) ->
            val parentRequired = parentPropSchemas[propName]?.required ?: false
            val effectiveRequired = parentRequired || info.required

            val overridden = parentPropSchemas.containsKey(propName)

            val newField = FieldDO(
                originalName = propName,
                generatedName = propName.toKotlinName(),
                overridden = overridden,
                type = info.type.withNullability(nullable = !effectiveRequired),
                required = effectiveRequired,
                defaultValueCode = info.defaultValueCode,
            )

            val existingIndex = fields.indexOfFirst { it.originalName == propName }
            if (existingIndex >= 0) fields[existingIndex] = newField else fields += newField
        }

        component.rawSchema.discriminatorPropertyName?.let { discName ->
            val alreadyPresent = fields.any { it.originalName == discName }
            if (!alreadyPresent) {
                fields.add(
                    0,
                    FieldDO(
                        originalName = discName,
                        generatedName = discName.toKotlinName(),
                        overridden = false,
                        type = TrivialTypeDO(TrivialTypeDO.Kind.STRING, nullable = false),
                        required = true,
                        defaultValueCode = null,
                    ),
                )
            }
        }

        component.fields = fields.toMutableList()
    }

    val byNameAfter = associateBy { it.rawSchema.originalName }

    forEach { component ->
        val parentName = when (val shape = component.modelShape) {
            is ModelShapeDO.DataClass -> shape.extend
            is ModelShapeDO.OpenClass -> shape.extend
            else -> null
        } ?: return@forEach

        val parent = byNameAfter[parentName] ?: return@forEach

        component.fields = component.fields.map { field ->
            val parentField = parent.fields.firstOrNull { it.generatedName == field.generatedName }

            val parentRequired = parentField?.required ?: false
            val finalRequired = parentRequired || field.required
            val finalType = field.type.withNullability(nullable = !finalRequired)

            field.copy(type = finalType, required = finalRequired)
        }.toMutableList()
    }
}

private data class PropInfo(
    val type: FieldTypeDO,
    val required: Boolean,
    val defaultValueCode: String?,
)

private fun collectAllPropertiesFromSchema(
    schemaName: String,
    schemas: Map<String, ModelDO>,
    into: MutableMap<String, PropInfo>,
    visited: MutableSet<String>,
) {
    if (!visited.add(schemaName)) return
    val schema = schemas[schemaName] ?: return

    schema.rawSchema.ownProperties.values.forEach { prop ->
        val info = PropInfo(
            type = prop.type.toFinalType(
                cfg = OpenApi2KotlinUseCase.ModelConfig.MappingConfig(),
                owner = schema,
                bySchemaName = schemas,
            ),
            required = prop.required,
            defaultValueCode = prop.defaultValue?.let { renderDefault(prop.type, OpenApi2KotlinUseCase.ModelConfig.MappingConfig(), it) },
        )
        into.merge(prop.name, info) { a, b ->
            PropInfo(
                type = a.type,
                required = a.required || b.required,
                defaultValueCode = a.defaultValueCode ?: b.defaultValueCode,
            )
        }
    }

    schema.rawSchema.allOfParents.forEach { parentName ->
        collectAllPropertiesFromSchema(parentName, schemas, into, visited)
    }
}

internal fun RawSchemaDO.RawFieldTypeDO.toFinalType(
    cfg: OpenApi2KotlinUseCase.ModelConfig.MappingConfig,
    owner: ModelDO,
    bySchemaName: Map<String, ModelDO>,
): FieldTypeDO = when (this) {
    is RawSchemaDO.RawRefTypeDO ->
        RefTypeDO(schemaName = schemaName, nullable = nullable)

    is RawSchemaDO.RawArrayTypeDO -> {
        val elem = elementType.toFinalType(cfg, owner, bySchemaName)
        ListTypeDO(elementType = elem, nullable = nullable)
    }

    is RawSchemaDO.RawPrimitiveTypeDO -> {
        val kind = toFinalTrivialKind(cfg)
        TrivialTypeDO(kind = kind, nullable = nullable)
    }
}

private fun RawSchemaDO.RawPrimitiveTypeDO.toFinalTrivialKind(
    cfg: OpenApi2KotlinUseCase.ModelConfig.MappingConfig,
): TrivialTypeDO.Kind {
    return when (type) {
        RawSchemaDO.RawPrimitiveTypeDO.Type.BOOLEAN ->
            TrivialTypeDO.Kind.BOOLEAN

        RawSchemaDO.RawPrimitiveTypeDO.Type.INTEGER -> {
            val isInt64 = format.equals("int64", ignoreCase = true)
            if (isInt64) TrivialTypeDO.Kind.LONG
            else if (cfg.integer2Long) TrivialTypeDO.Kind.LONG
            else TrivialTypeDO.Kind.INT
        }

        RawSchemaDO.RawPrimitiveTypeDO.Type.NUMBER -> {
            val isFloat = format.equals("float", ignoreCase = true)
            val isDouble = format == null || format.equals("double", ignoreCase = true)

            when {
                isFloat && cfg.float2BigDecimal -> TrivialTypeDO.Kind.BIG_DECIMAL
                isDouble && cfg.double2BigDecimal -> TrivialTypeDO.Kind.BIG_DECIMAL
                isFloat -> TrivialTypeDO.Kind.FLOAT
                else -> TrivialTypeDO.Kind.DOUBLE
            }
        }

        RawSchemaDO.RawPrimitiveTypeDO.Type.STRING -> {
            when (format?.lowercase()) {
                "date" -> TrivialTypeDO.Kind.LOCAL_DATE
                "date-time" -> TrivialTypeDO.Kind.OFFSET_DATE_TIME
                "binary", "byte" -> TrivialTypeDO.Kind.BYTE_ARRAY
                else -> TrivialTypeDO.Kind.STRING
            }
        }

        RawSchemaDO.RawPrimitiveTypeDO.Type.OBJECT ->
            TrivialTypeDO.Kind.ANY
    }
}

internal fun renderDefault(
    rawType: RawSchemaDO.RawFieldTypeDO,
    cfg: OpenApi2KotlinUseCase.ModelConfig.MappingConfig,
    rawDefault: String,
): String {
    val finalType = rawType.toFinalType(cfg, owner = dummyOwner, bySchemaName = emptyMap())
    return renderDefaultForFinalType(finalType, rawDefault)
}

private val dummyOwner = ModelDO(
    rawSchema = RawSchemaDO(
        originalName = "_",
        allOfParents = emptyList(),
        oneOfChildren = emptyList(),
    ),
    packageName = "_",
    generatedName = "_",
)

internal fun renderDefaultForFinalType(
    finalType: FieldTypeDO,
    rawDefault: String,
): String {
    return when (finalType) {
        is TrivialTypeDO -> when (finalType.kind) {
            TrivialTypeDO.Kind.STRING ->
                quote(rawDefault)

            TrivialTypeDO.Kind.INT ->
                rawDefault

            TrivialTypeDO.Kind.LONG ->
                suffixIfMissing(rawDefault, "L")

            TrivialTypeDO.Kind.FLOAT ->
                suffixIfMissing(rawDefault, "f")

            TrivialTypeDO.Kind.DOUBLE ->
                rawDefault

            TrivialTypeDO.Kind.BIG_DECIMAL ->
                "BigDecimal(${quote(rawDefault)})"

            TrivialTypeDO.Kind.BOOLEAN ->
                rawDefault.lowercase()

            TrivialTypeDO.Kind.LOCAL_DATE ->
                "LocalDate.parse(${quote(rawDefault)})"

            TrivialTypeDO.Kind.OFFSET_DATE_TIME ->
                "OffsetDateTime.parse(${quote(rawDefault)})"

            TrivialTypeDO.Kind.BYTE_ARRAY ->
                rawDefault

            TrivialTypeDO.Kind.ANY ->
                rawDefault
        }

        is RefTypeDO ->
            rawDefault

        is ListTypeDO ->
            rawDefault
    }
}

private fun quote(s: String): String = buildString {
    append('"')
    for (ch in s) {
        when (ch) {
            '\\' -> append("\\\\")
            '"' -> append("\\\"")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> append(ch)
        }
    }
    append('"')
}

private fun suffixIfMissing(s: String, suffix: String): String {
    val trimmed = s.trim()
    return if (trimmed.endsWith(suffix, ignoreCase = true)) trimmed else trimmed + suffix
}