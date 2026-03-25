package dev.openapi2kotlin.adapter.generateserver.ktor.internal

import com.squareup.kotlinpoet.CodeBlock
import dev.openapi2kotlin.application.core.openapi2kotlin.model.api.ApiParamDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.ListTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.TrivialTypeDO

internal fun pathReadExpr(
    param: ApiParamDO,
    label: String,
): CodeBlock {
    val key = param.rawParam.name
    return when ((param.type as? TrivialTypeDO)?.kind) {
        TrivialTypeDO.Kind.STRING, null ->
            badRequestPathExpr(key, label, "call.parameters[%S]")
        TrivialTypeDO.Kind.LONG ->
            badRequestPathExpr(key, label, "call.parameters[%S]?.toLongOrNull()")
        TrivialTypeDO.Kind.INT ->
            badRequestPathExpr(key, label, "call.parameters[%S]?.toIntOrNull()")
        TrivialTypeDO.Kind.FLOAT, TrivialTypeDO.Kind.DOUBLE ->
            badRequestPathExpr(key, label, "call.parameters[%S]?.toDoubleOrNull()")
        TrivialTypeDO.Kind.BOOLEAN ->
            badRequestPathExpr(key, label, "call.parameters[%S]?.toBooleanStrictOrNull()")
        else ->
            badRequestPathExpr(key, label, "call.parameters[%S]")
    }
}

internal fun queryReadExpr(param: ApiParamDO): CodeBlock {
    val key = param.rawParam.name
    return when (param.type) {
        is ListTypeDO -> listQueryReadExpr(key, param.type as ListTypeDO)
        else -> scalarQueryReadExpr(key, (param.type as? TrivialTypeDO)?.kind)
    }
}

internal fun headerReadExpr(param: ApiParamDO): CodeBlock {
    val key = param.rawParam.name
    return when ((param.type as? TrivialTypeDO)?.kind) {
        TrivialTypeDO.Kind.STRING, null -> CodeBlock.of("call.request.headers[%S]", key)
        TrivialTypeDO.Kind.LONG -> CodeBlock.of("call.request.headers[%S]?.toLongOrNull()", key)
        TrivialTypeDO.Kind.INT -> CodeBlock.of("call.request.headers[%S]?.toIntOrNull()", key)
        TrivialTypeDO.Kind.FLOAT, TrivialTypeDO.Kind.DOUBLE ->
            CodeBlock.of("call.request.headers[%S]?.toDoubleOrNull()", key)
        TrivialTypeDO.Kind.BOOLEAN ->
            CodeBlock.of("call.request.headers[%S]?.toBooleanStrictOrNull()", key)
        else -> CodeBlock.of("call.request.headers[%S]", key)
    }
}

private fun listQueryReadExpr(
    key: String,
    listType: ListTypeDO,
): CodeBlock {
    val elementKind = (listType.elementType as? TrivialTypeDO)?.kind
    return when (elementKind) {
        TrivialTypeDO.Kind.LONG -> listQueryParseExpr(key, "String::toLongOrNull")
        TrivialTypeDO.Kind.INT -> listQueryParseExpr(key, "String::toIntOrNull")
        TrivialTypeDO.Kind.FLOAT, TrivialTypeDO.Kind.DOUBLE ->
            listQueryParseExpr(key, "String::toDoubleOrNull")
        TrivialTypeDO.Kind.BOOLEAN ->
            listQueryParseExpr(key, "String::toBooleanStrictOrNull")
        else -> CodeBlock.of("call.request.queryParameters.getAll(%S)?.takeIf { it.isNotEmpty() }", key)
    }
}

private fun scalarQueryReadExpr(
    key: String,
    kind: TrivialTypeDO.Kind?,
): CodeBlock =
    when (kind) {
        TrivialTypeDO.Kind.STRING, null -> CodeBlock.of("call.request.queryParameters[%S]", key)
        TrivialTypeDO.Kind.LONG -> CodeBlock.of("call.request.queryParameters[%S]?.toLongOrNull()", key)
        TrivialTypeDO.Kind.INT -> CodeBlock.of("call.request.queryParameters[%S]?.toIntOrNull()", key)
        TrivialTypeDO.Kind.FLOAT, TrivialTypeDO.Kind.DOUBLE ->
            CodeBlock.of("call.request.queryParameters[%S]?.toDoubleOrNull()", key)
        TrivialTypeDO.Kind.BOOLEAN ->
            CodeBlock.of("call.request.queryParameters[%S]?.toBooleanStrictOrNull()", key)
        else -> CodeBlock.of("call.request.queryParameters[%S]", key)
    }

private fun badRequestPathExpr(
    key: String,
    label: String,
    valueExpr: String,
): CodeBlock =
    CodeBlock.of(
        "$valueExpr ?: return@%L call.%M(%T.BadRequest)",
        key,
        label,
        M_respond,
        HTTP_STATUS_T,
    )

private fun listQueryParseExpr(
    key: String,
    parserRef: String,
): CodeBlock =
    CodeBlock.of(
        "call.request.queryParameters.getAll(%S)?.mapNotNull(%L)?.takeIf { it.isNotEmpty() }",
        key,
        parserRef,
    )
