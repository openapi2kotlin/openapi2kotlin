package dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.api.helpers

import dev.openapi2kotlin.application.core.openapi2kotlin.model.api.*
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.FieldTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.raw.RawPathDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.raw.RawSchemaDO
import dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.common.toFinalType
import dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.common.withNullability

internal fun List<RawPathDO>.toApis(
    ctx: ApisContext,
): List<ApiDO> =
    map { rawPath ->
        ApiDO(
            rawPath = rawPath,
            generatedName = rawPath.generatedApiName(),
            endpoints = rawPath.operations.map { it.toBaseEndpoint(ctx) },
        )
    }

private fun RawPathDO.generatedApiName(): String {
    val tag = tags.firstOrNull().orEmpty().ifBlank { "Default" }
    return tag
        .replace("[^A-Za-z0-9]".toRegex(), " ")
        .trim()
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .joinToString("") { it.replaceFirstChar(Char::uppercase) } + "Api"
}

private fun RawPathDO.OperationDO.toBaseEndpoint(
    ctx: ApisContext,
): ApiEndpointDO {
    val opName = (operationId ?: inferOperationId(httpMethod, path)).toKotlinIdentifier()

    val params = parameters.map { p ->
        ApiParamDO(
            rawParam = p,
            generatedName = p.name.toKotlinParamName(),
            type = p.type.toApiFieldType(ctx, required = p.required),
        )
    }

    val body = requestBody?.let { rb ->
        ApiRequestBodyDO(
            generatedName = "body",
            type = rb.type.toApiFieldType(ctx, required = rb.required),
        )
    }

    val success = responses
        ?.firstOrNull { it.statusCode in setOf(200, 201, 202, 204) }
        ?.let { r ->
            ApiSuccessResponseDO(
                rawResponse = r,
                type = r.type?.toApiFieldType(ctx, required = true),
            )
        }

    return ApiEndpointDO(
        rawOperation = this,
        generatedName = opName,
        params = params,
        requestBody = body,
        successResponse = success,
    )
}

private fun inferOperationId(method: RawPathDO.HttpMethodDO, path: String): String {
    val cleanPath = path.trim('/').replace("/", "_").replace("{", "").replace("}", "")
    return method.name.lowercase() + cleanPath.replaceFirstChar { it.uppercase() }
}

private fun String.toKotlinParamName(): String =
    splitIdentifierWords()
        .takeIf { it.isNotEmpty() }
        ?.let { parts ->
            parts.first().lowercase() + parts.drop(1).joinToString("") {
                it.lowercase().replaceFirstChar(Char::uppercase)
            }
        }
        ?: "param"

private fun String.splitIdentifierWords(): List<String> {
    val tokens = mutableListOf<String>()
    val current = StringBuilder()

    fun flush() {
        if (current.isNotEmpty()) {
            tokens += current.toString()
            current.setLength(0)
        }
    }

    for (i in indices) {
        val c = this[i]

        if (!c.isLetterOrDigit()) {
            flush()
            continue
        }

        val prev = getOrNull(i - 1)
        val next = getOrNull(i + 1)

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
                (prevIsLower && cIsUpper) ||
                    (prevIsUpper && cIsUpper && nextIsLower) ||
                    (prevIsLetter && cIsDigit) ||
                    (prevIsDigit && (cIsUpper || cIsLower))
                )

        if (boundary) flush()
        current.append(c)
    }

    flush()
    return tokens.filter { it.isNotBlank() }
}

private fun String.toKotlinIdentifier(): String =
    replace("[^A-Za-z0-9_]".toRegex(), "_")
        .replace(Regex("_+"), "_")
        .trim('_')
        .ifBlank { "op" }


private fun RawSchemaDO.RawFieldTypeDO.toApiFieldType(
    ctx: ApisContext,
    required: Boolean,
): FieldTypeDO {
    val base = toFinalType(cfg = ctx.mappingCfg, annotationsCfg = ctx.annotationsCfg)
    val finalNullable = base.nullable || !required
    return base.withNullability(finalNullable)
}