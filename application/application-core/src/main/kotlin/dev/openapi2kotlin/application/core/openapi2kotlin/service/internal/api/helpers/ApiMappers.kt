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
    val successResponseType = responses
        ?.firstOrNull { it.statusCode in setOf(200, 201, 202, 204) }
        ?.type

    val opName = generatedOperationName(
        ctx = ctx,
        method = httpMethod,
        operationId = operationId,
        path = path,
        successResponseType = successResponseType,
    )

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

private enum class OperationVerb(val kotlinName: String) {
    RETRIEVE("retrieve"),
    LIST("list"),
    CREATE("create"),
    UPDATE("update"),
    PATCH("patch"),
    DELETE("delete"),
}

private fun generatedOperationName(
    ctx: ApisContext,
    method: RawPathDO.HttpMethodDO,
    operationId: String?,
    path: String,
    successResponseType: RawSchemaDO.RawFieldTypeDO?,
): String {
    if (ctx.apiCfg?.methodNameFromOperationId == true) {
        return generatedOperationNameFromOperationId(
            method = method,
            operationId = operationId,
            path = path,
        )
    }

    val preferredVerb = preferredVerb(
        method = method,
        path = path,
        successResponseType = successResponseType,
    )
    val resourceTokens = path
        .resourceNameSegments(
            verb = preferredVerb,
            successResponseType = successResponseType,
            singularized = ctx.apiCfg?.methodNameSingularized ?: true,
            pluralized = ctx.apiCfg?.methodNamePluralized ?: true,
        )
        .flatMap { it.splitIdentifierWords() }
        .map { it.lowercase() }

    val nameTokens = listOf(preferredVerb.kotlinName) + resourceTokens
    return nameTokens
        .toLowerCamel()
        .ensureKotlinIdentifier(defaultName = preferredVerb.kotlinName)
}

private fun generatedOperationNameFromOperationId(
    method: RawPathDO.HttpMethodDO,
    operationId: String?,
    path: String,
): String {
    val source = operationId ?: path.trim('/').replace("/", "_").replace("{", "").replace("}", "")
    val rawTokens = source.splitIdentifierWords()
    if (rawTokens.isEmpty()) return fallbackVerb(method).kotlinName

    return rawTokens
        .map { it.lowercase() }
        .toLowerCamel()
        .ensureKotlinIdentifier(defaultName = fallbackVerb(method).kotlinName)
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

private fun List<String>.toLowerCamel(): String =
    firstOrNull()?.lowercase().orEmpty() +
        drop(1).joinToString("") { it.lowercase().replaceFirstChar(Char::uppercase) }

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

private fun String.ensureKotlinIdentifier(defaultName: String): String =
    replace("[^A-Za-z0-9_]".toRegex(), "_")
        .replace(Regex("_+"), "_")
        .trim('_')
        .ifBlank { defaultName }
        .let { if (it.first().isDigit()) "_$it" else it }

private fun fallbackVerb(method: RawPathDO.HttpMethodDO): OperationVerb = when (method) {
    RawPathDO.HttpMethodDO.GET -> OperationVerb.RETRIEVE
    RawPathDO.HttpMethodDO.POST -> OperationVerb.CREATE
    RawPathDO.HttpMethodDO.PUT -> OperationVerb.UPDATE
    RawPathDO.HttpMethodDO.PATCH -> OperationVerb.PATCH
    RawPathDO.HttpMethodDO.DELETE -> OperationVerb.DELETE
}

private fun preferredVerb(
    method: RawPathDO.HttpMethodDO,
    path: String,
    successResponseType: RawSchemaDO.RawFieldTypeDO?,
): OperationVerb {
    if (method != RawPathDO.HttpMethodDO.GET) return fallbackVerb(method)

    val isCollection =
        successResponseType is RawSchemaDO.RawArrayTypeDO ||
            path.looksLikeCollectionEndpoint()

    return if (isCollection) OperationVerb.LIST else OperationVerb.RETRIEVE
}

private fun String.looksLikeCollectionEndpoint(): Boolean {
    val segments = trim('/').split('/').filter { it.isNotBlank() }
    if (segments.isEmpty()) return false

    val lastSegment = segments.last()
    if (lastSegment.startsWith("{") && lastSegment.endsWith("}")) return false

    return lastSegment.isLikelyPlural()
}

private fun String.resourceNameSegments(
    verb: OperationVerb,
    successResponseType: RawSchemaDO.RawFieldTypeDO?,
    singularized: Boolean,
    pluralized: Boolean,
): List<String> {
    val segments = trim('/').split('/').filter { it.isNotBlank() }
    if (segments.isEmpty()) return emptyList()

    val lastIsParam = segments.last().isPathParam()
    val staticSegments = segments.filterNot { it.isPathParam() }
    if (staticSegments.isEmpty()) return emptyList()

    val lastStatic = staticSegments.last()
    val previousStatic = staticSegments.dropLast(1).lastOrNull()
    val isCollection = successResponseType is RawSchemaDO.RawArrayTypeDO || looksLikeCollectionEndpoint()

    return when (verb) {
        OperationVerb.LIST ->
            listOf(if (pluralized) lastStatic.pluralize() else lastStatic)

        OperationVerb.RETRIEVE,
        OperationVerb.UPDATE,
        OperationVerb.DELETE -> when {
            lastIsParam -> listOf((previousStatic ?: lastStatic).transformSingularized(singularized))
            else -> listOf(lastStatic.transformSingularized(singularized))
        }

        OperationVerb.CREATE -> when {
            lastIsParam && previousStatic != null ->
                listOf(
                    previousStatic.transformSingularized(singularized),
                    lastStatic.transformSingularized(singularized),
                )
            isCollection -> listOf(lastStatic.transformSingularized(singularized))
            else -> listOf(lastStatic.transformSingularized(singularized))
        }

        OperationVerb.PATCH -> when {
            lastIsParam -> listOf((previousStatic ?: lastStatic).transformSingularized(singularized))
            else -> listOf(lastStatic.transformSingularized(singularized))
        }
    }
}

private fun String.isPathParam(): Boolean =
    startsWith("{") && endsWith("}")

private fun String.isLikelyPlural(): Boolean {
    val normalized = lowercase()
    return normalized.endsWith("s") && !normalized.endsWith("ss")
}

private fun String.singularize(): String {
    val normalized = trim()
    return when {
        normalized.endsWith("ies", ignoreCase = true) && normalized.length > 3 ->
            normalized.dropLast(3) + "y"
        normalized.endsWith("ses", ignoreCase = true) && normalized.length > 3 ->
            normalized.dropLast(2)
        normalized.endsWith("s", ignoreCase = true) &&
            !normalized.endsWith("ss", ignoreCase = true) &&
            normalized.length > 1 -> normalized.dropLast(1)
        else -> normalized
    }
}

private fun String.pluralize(): String {
    val normalized = trim()
    return when {
        normalized.endsWith("ies", ignoreCase = true) -> normalized
        normalized.endsWith("s", ignoreCase = true) -> normalized
        normalized.endsWith("y", ignoreCase = true) && normalized.length > 1 ->
            normalized.dropLast(1) + "ies"
        normalized.endsWith("x", ignoreCase = true) ||
            normalized.endsWith("z", ignoreCase = true) ||
            normalized.endsWith("ch", ignoreCase = true) ||
            normalized.endsWith("sh", ignoreCase = true) -> normalized + "es"
        else -> normalized + "s"
    }
}

private fun String.transformSingularized(enabled: Boolean): String =
    if (enabled) singularize() else this


private fun RawSchemaDO.RawFieldTypeDO.toApiFieldType(
    ctx: ApisContext,
    required: Boolean,
): FieldTypeDO {
    val base = toFinalType(cfg = ctx.modelCfg)
    val finalNullable = base.nullable || !required
    return base.withNullability(finalNullable)
}
