package dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.common

import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.FieldTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.ListTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.RefTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.TrivialTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.raw.RawSchemaDO
import dev.openapi2kotlin.application.usecase.openapi2kotlin.OpenApi2KotlinUseCase

internal fun FieldTypeDO.withNullability(nullable: Boolean): FieldTypeDO =
    when (this) {
        is TrivialTypeDO -> copy(nullable = nullable)
        is RefTypeDO -> copy(nullable = nullable)
        is ListTypeDO -> copy(nullable = nullable)
    }

internal fun RawSchemaDO.RawFieldTypeDO.toFinalType(cfg: OpenApi2KotlinUseCase.ModelConfig): FieldTypeDO =
    when (this) {
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
    cfg: OpenApi2KotlinUseCase.ModelConfig,
): TrivialTypeDO.Kind =
    when (type) {
        RawSchemaDO.RawPrimitiveTypeDO.Type.BOOLEAN -> TrivialTypeDO.Kind.BOOLEAN
        RawSchemaDO.RawPrimitiveTypeDO.Type.INTEGER -> toIntegerKind(cfg)
        RawSchemaDO.RawPrimitiveTypeDO.Type.NUMBER -> toNumberKind(cfg)
        RawSchemaDO.RawPrimitiveTypeDO.Type.STRING -> toStringKind(cfg)
        RawSchemaDO.RawPrimitiveTypeDO.Type.OBJECT -> toObjectKind(cfg)
    }

internal fun renderDefault(
    rawType: RawSchemaDO.RawFieldTypeDO,
    cfg: OpenApi2KotlinUseCase.ModelConfig,
    rawDefault: String,
): String {
    val finalType = rawType.toFinalType(cfg)
    return renderDefaultForFinalType(finalType, rawDefault)
}

private fun RawSchemaDO.RawPrimitiveTypeDO.toIntegerKind(cfg: OpenApi2KotlinUseCase.ModelConfig): TrivialTypeDO.Kind =
    when {
        format.equals("int64", ignoreCase = true) -> TrivialTypeDO.Kind.LONG
        cfg.integer2Long -> TrivialTypeDO.Kind.LONG
        else -> TrivialTypeDO.Kind.INT
    }

private fun RawSchemaDO.RawPrimitiveTypeDO.toNumberKind(cfg: OpenApi2KotlinUseCase.ModelConfig): TrivialTypeDO.Kind {
    val isFloat = format.equals("float", ignoreCase = true)
    val isDouble = format == null || format.equals("double", ignoreCase = true)

    return when {
        isFloat && cfg.float2BigDecimal -> TrivialTypeDO.Kind.BIG_DECIMAL
        isDouble && cfg.double2BigDecimal -> TrivialTypeDO.Kind.BIG_DECIMAL
        isFloat -> TrivialTypeDO.Kind.FLOAT
        else -> TrivialTypeDO.Kind.DOUBLE
    }
}

private fun RawSchemaDO.RawPrimitiveTypeDO.toStringKind(cfg: OpenApi2KotlinUseCase.ModelConfig): TrivialTypeDO.Kind =
    when (format?.lowercase()) {
        "date" -> cfg.dateKind()
        "date-time" -> cfg.dateTimeKind()
        "binary", "byte" -> TrivialTypeDO.Kind.BYTE_ARRAY
        else -> TrivialTypeDO.Kind.STRING
    }

private fun RawSchemaDO.RawPrimitiveTypeDO.toObjectKind(cfg: OpenApi2KotlinUseCase.ModelConfig): TrivialTypeDO.Kind =
    when (cfg.serialization) {
        OpenApi2KotlinUseCase.ModelConfig.Serialization.KOTLINX -> TrivialTypeDO.Kind.JSON_ELEMENT
        else -> TrivialTypeDO.Kind.ANY
    }

private fun OpenApi2KotlinUseCase.ModelConfig.dateKind(): TrivialTypeDO.Kind =
    when (serialization) {
        OpenApi2KotlinUseCase.ModelConfig.Serialization.KOTLINX ->
            TrivialTypeDO.Kind.KOTLINX_LOCAL_DATE

        else -> TrivialTypeDO.Kind.JAVA_LOCAL_DATE
    }

private fun OpenApi2KotlinUseCase.ModelConfig.dateTimeKind(): TrivialTypeDO.Kind =
    when (serialization) {
        OpenApi2KotlinUseCase.ModelConfig.Serialization.KOTLINX ->
            TrivialTypeDO.Kind.INSTANT

        else -> TrivialTypeDO.Kind.OFFSET_DATE_TIME
    }
