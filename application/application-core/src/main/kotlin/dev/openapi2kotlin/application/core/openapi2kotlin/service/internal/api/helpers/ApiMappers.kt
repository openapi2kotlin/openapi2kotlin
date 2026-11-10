package dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.api.helpers

import dev.openapi2kotlin.application.core.openapi2kotlin.domain.api.ApiDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.api.ApiEndpointDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.api.ApiParamDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.api.ApiRequestBodyDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.api.ApiSuccessResponseDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.model.FieldTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.raw.RawPathDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.raw.RawSchemaDO
import dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.common.toFinalType
import dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.common.withNullability

private const val HTTP_OK = 200
private const val HTTP_CREATED = 201
private const val HTTP_ACCEPTED = 202
private const val HTTP_NO_CONTENT = 204

private val SUCCESS_STATUS_CODES = setOf(HTTP_OK, HTTP_CREATED, HTTP_ACCEPTED, HTTP_NO_CONTENT)

internal fun List<RawPathDO>.toApis(ctx: ApisContext): List<ApiDO> =
    map { rawPath ->
        ApiDO(
            rawPath = rawPath,
            generatedName = rawPath.generatedApiName(),
            endpoints = rawPath.operations.map { it.toBaseEndpoint(ctx) },
        )
    }.disambiguateClashingEndpoints()

/**
 * Path-derived names can collide for nested resources, e.g. `GET /a/{id}/b/{bId}` and
 * `GET /a/{id}/c/{cId}` both become `retrieveA(Long, Long)`. Kotlin overloads with different
 * parameter types stay as they are; endpoints whose name and parameter types clash within one
 * generated API fall back to their operationId-derived name.
 */
private fun List<ApiDO>.disambiguateClashingEndpoints(): List<ApiDO> {
    val clashing =
        flatMap { api -> api.endpoints.map { api.generatedName to it } }
            .groupBy { (apiName, endpoint) -> Triple(apiName, endpoint.generatedName, endpoint.signatureKey()) }
            .values
            .filter { it.size > 1 }
            .flatMap { group -> group.map { it.second.rawOperation } }
            .toSet()
    if (clashing.isEmpty()) return this

    return map { api ->
        api.copy(
            endpoints =
                api.endpoints.map { endpoint ->
                    if (endpoint.rawOperation in clashing) {
                        endpoint.copy(
                            generatedName =
                                generatedOperationNameFromOperationId(
                                    method = endpoint.rawOperation.httpMethod,
                                    operationId = endpoint.rawOperation.operationId,
                                    path = endpoint.rawOperation.path,
                                ),
                        )
                    } else {
                        endpoint
                    }
                },
        )
    }
}

private fun ApiEndpointDO.signatureKey(): List<Any?> =
    rawOperation.parameters.map { it.type } + listOf(rawOperation.requestBody?.type)

private fun RawPathDO.generatedApiName(): String {
    val tag = tags.firstOrNull().orEmpty().ifBlank { "Default" }
    return tag
        .replace("[^A-Za-z0-9]".toRegex(), " ")
        .trim()
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .joinToString("") { it.replaceFirstChar(Char::uppercase) } + "Api"
}

private fun RawPathDO.OperationDO.toBaseEndpoint(ctx: ApisContext): ApiEndpointDO {
    val successResponseType =
        responses
            ?.firstOrNull { it.statusCode in SUCCESS_STATUS_CODES }
            ?.type

    val opName =
        generatedOperationName(
            ctx = ctx,
            method = httpMethod,
            operationId = operationId,
            path = path,
            successResponseType = successResponseType,
        )

    val params =
        parameters.map { p ->
            ApiParamDO(
                rawParam = p,
                generatedName = p.name.toKotlinParamName(),
                type = p.type.toApiFieldType(ctx, required = p.required),
            )
        }

    val body =
        requestBody?.let { rb ->
            ApiRequestBodyDO(
                generatedName = "body",
                type = rb.type.toApiFieldType(ctx, required = rb.required),
            )
        }

    val success =
        responses
            ?.firstOrNull { it.statusCode in SUCCESS_STATUS_CODES }
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

internal enum class OperationVerb(
    val kotlinName: String,
) {
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

    val preferredVerb =
        preferredVerb(
            method = method,
            path = path,
            successResponseType = successResponseType,
        )
    val resourceTokens =
        path
            .resourceNameSegments(
                verb = preferredVerb,
                successResponseType = successResponseType,
                singularized = ctx.apiCfg?.methodNameSingularized ?: true,
                pluralized = ctx.apiCfg?.methodNamePluralized ?: true,
            ).flatMap { it.splitIdentifierWords() }
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
    val source =
        operationId ?: path
            .trim('/')
            .replace("/", "_")
            .replace("{", "")
            .replace("}", "")
    val rawTokens = source.splitIdentifierWords()
    if (rawTokens.isEmpty()) return fallbackVerb(method).kotlinName

    return rawTokens
        .map { it.lowercase() }
        .toLowerCamel()
        .ensureKotlinIdentifier(defaultName = fallbackVerb(method).kotlinName)
}

private fun String.toKotlinParamName(): String {
    val parts = splitIdentifierWords()
    val name =
        if (parts.isEmpty()) {
            "param"
        } else {
            parts.first().lowercase() +
                parts.drop(1).joinToString("") { it.lowercase().replaceFirstChar(Char::uppercase) }
        }
    return if (name in KOTLIN_HARD_KEYWORDS) "${name}_" else name
}

private val KOTLIN_HARD_KEYWORDS =
    setOf(
        "as", "break", "class", "continue", "do", "else", "false", "for", "fun", "if", "in",
        "interface", "is", "null", "object", "package", "return", "super", "this", "throw",
        "true", "try", "typealias", "typeof", "val", "var", "when", "while",
    )

private fun List<String>.toLowerCamel(): String =
    firstOrNull()?.lowercase().orEmpty() +
        drop(1).joinToString("") { it.lowercase().replaceFirstChar(Char::uppercase) }

private fun fallbackVerb(method: RawPathDO.HttpMethodDO): OperationVerb =
    when (method) {
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

private fun RawSchemaDO.RawFieldTypeDO.toApiFieldType(
    ctx: ApisContext,
    required: Boolean,
): FieldTypeDO {
    val base = toFinalType(cfg = ctx.modelCfg)
    val finalNullable = base.nullable || !required
    return base.withNullability(finalNullable)
}
