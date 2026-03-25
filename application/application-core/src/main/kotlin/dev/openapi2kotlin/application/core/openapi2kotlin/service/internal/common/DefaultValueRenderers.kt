package dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.common

import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.FieldTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.ListTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.RefTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.TrivialTypeDO

internal fun renderDefaultForFinalType(
    finalType: FieldTypeDO,
    rawDefault: String,
): String =
    when (finalType) {
        is TrivialTypeDO -> renderTrivialDefault(finalType.kind, rawDefault)
        is RefTypeDO -> rawDefault
        is ListTypeDO -> rawDefault
    }

private fun renderTrivialDefault(
    kind: TrivialTypeDO.Kind,
    rawDefault: String,
): String =
    when (kind) {
        TrivialTypeDO.Kind.STRING -> quote(rawDefault)
        TrivialTypeDO.Kind.INT -> rawDefault
        TrivialTypeDO.Kind.LONG -> suffixIfMissing(rawDefault, "L")
        TrivialTypeDO.Kind.FLOAT -> suffixIfMissing(rawDefault, "f")
        TrivialTypeDO.Kind.DOUBLE -> rawDefault
        else -> renderStructuredDefault(kind, rawDefault)
    }

private fun renderStructuredDefault(
    kind: TrivialTypeDO.Kind,
    rawDefault: String,
): String =
    when (kind) {
        TrivialTypeDO.Kind.BIG_DECIMAL -> "BigDecimal(${quote(rawDefault)})"
        TrivialTypeDO.Kind.BOOLEAN -> rawDefault.lowercase()
        TrivialTypeDO.Kind.JAVA_LOCAL_DATE -> "LocalDate.parse(${quote(rawDefault)})"
        TrivialTypeDO.Kind.KOTLINX_LOCAL_DATE -> "LocalDate.parse(${quote(rawDefault)})"
        TrivialTypeDO.Kind.OFFSET_DATE_TIME -> "OffsetDateTime.parse(${quote(rawDefault)})"
        TrivialTypeDO.Kind.INSTANT -> "Instant.parse(${quote(rawDefault)})"
        TrivialTypeDO.Kind.BYTE_ARRAY -> rawDefault
        TrivialTypeDO.Kind.JSON_ELEMENT -> rawDefault
        TrivialTypeDO.Kind.ANY -> rawDefault
        TrivialTypeDO.Kind.STRING,
        TrivialTypeDO.Kind.INT,
        TrivialTypeDO.Kind.LONG,
        TrivialTypeDO.Kind.FLOAT,
        TrivialTypeDO.Kind.DOUBLE,
        -> rawDefault
    }

private fun quote(s: String): String =
    buildString {
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

private fun suffixIfMissing(
    s: String,
    suffix: String,
): String {
    val trimmed = s.trim()
    return if (trimmed.endsWith(suffix, ignoreCase = true)) trimmed else trimmed + suffix
}
