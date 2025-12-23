package dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.common

import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.FieldTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.ListTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.RefTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.TrivialTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.raw.RawSchemaDO
import dev.openapi2kotlin.application.usecase.openapi2kotlin.OpenApi2KotlinUseCase

internal fun FieldTypeDO.withNullability(nullable: Boolean): FieldTypeDO = when (this) {
    is TrivialTypeDO -> copy(nullable = nullable)
    is RefTypeDO -> copy(nullable = nullable)
    is ListTypeDO -> copy(nullable = nullable)
}

internal fun RawSchemaDO.RawFieldTypeDO.toFinalType(
    cfg: OpenApi2KotlinUseCase.ModelConfig.MappingConfig,
): FieldTypeDO = when (this) {
    is RawSchemaDO.RawRefTypeDO ->
        RefTypeDO(schemaName = schemaName, nullable = nullable)

    is RawSchemaDO.RawArrayTypeDO ->
        ListTypeDO(
            elementType = elementType.toFinalType(cfg),
            nullable = nullable,
        )

    is RawSchemaDO.RawPrimitiveTypeDO ->
        TrivialTypeDO(
            kind = toFinalTrivialKind(cfg),
            nullable = nullable,
        )
}

private fun RawSchemaDO.RawPrimitiveTypeDO.toFinalTrivialKind(
    cfg: OpenApi2KotlinUseCase.ModelConfig.MappingConfig,
): TrivialTypeDO.Kind = when (type) {
    RawSchemaDO.RawPrimitiveTypeDO.Type.BOOLEAN ->
        TrivialTypeDO.Kind.BOOLEAN

    RawSchemaDO.RawPrimitiveTypeDO.Type.INTEGER -> {
        val isInt64 = format.equals("int64", ignoreCase = true)
        when {
            isInt64 -> TrivialTypeDO.Kind.LONG
            cfg.integer2Long -> TrivialTypeDO.Kind.LONG
            else -> TrivialTypeDO.Kind.INT
        }
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

    RawSchemaDO.RawPrimitiveTypeDO.Type.STRING ->
        when (format?.lowercase()) {
            "date" -> TrivialTypeDO.Kind.LOCAL_DATE
            "date-time" -> TrivialTypeDO.Kind.OFFSET_DATE_TIME
            "binary", "byte" -> TrivialTypeDO.Kind.BYTE_ARRAY
            else -> TrivialTypeDO.Kind.STRING
        }

    RawSchemaDO.RawPrimitiveTypeDO.Type.OBJECT ->
        TrivialTypeDO.Kind.ANY
}

internal fun renderDefault(
    rawType: RawSchemaDO.RawFieldTypeDO,
    cfg: OpenApi2KotlinUseCase.ModelConfig.MappingConfig,
    rawDefault: String,
): String {
    val finalType = rawType.toFinalType(cfg)
    return renderDefaultForFinalType(finalType, rawDefault)
}

internal fun renderDefaultForFinalType(
    finalType: FieldTypeDO,
    rawDefault: String,
): String = when (finalType) {
    is TrivialTypeDO -> when (finalType.kind) {
        TrivialTypeDO.Kind.STRING -> quote(rawDefault)
        TrivialTypeDO.Kind.INT -> rawDefault
        TrivialTypeDO.Kind.LONG -> suffixIfMissing(rawDefault, "L")
        TrivialTypeDO.Kind.FLOAT -> suffixIfMissing(rawDefault, "f")
        TrivialTypeDO.Kind.DOUBLE -> rawDefault
        TrivialTypeDO.Kind.BIG_DECIMAL -> "BigDecimal(${quote(rawDefault)})"
        TrivialTypeDO.Kind.BOOLEAN -> rawDefault.lowercase()
        TrivialTypeDO.Kind.LOCAL_DATE -> "LocalDate.parse(${quote(rawDefault)})"
        TrivialTypeDO.Kind.OFFSET_DATE_TIME -> "OffsetDateTime.parse(${quote(rawDefault)})"
        TrivialTypeDO.Kind.BYTE_ARRAY -> rawDefault
        TrivialTypeDO.Kind.ANY -> rawDefault
    }

    is RefTypeDO ->
        rawDefault

    is ListTypeDO ->
        rawDefault
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
