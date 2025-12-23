package dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.server.helpers

import dev.openapi2kotlin.application.core.openapi2kotlin.model.raw.RawPathDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.server.*
import dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.server.ServerPrepareContext

internal fun List<RawPathDO>.toBaseServerApis(
    ctx: ServerPrepareContext,
): List<ServerApiDO> =
    map { rawPath ->
        ServerApiDO(
            rawPath = rawPath,
            generatedName = rawPath.generatedApiName(),
            endpoints = rawPath.operations.map { it.toBaseEndpoint(ctx) },
            annotations = emptyList(),
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
    ctx: ServerPrepareContext,
): ServerEndpointDO {
    val opName = (operationId ?: inferOperationId(httpMethod, path)).toKotlinIdentifier()

    val params = parameters.map { p ->
        ServerParamDO(
            rawParam = p,
            generatedName = p.name.toKotlinParamName(),
            type = p.type.toServerFieldType(ctx, required = p.required),
            annotations = emptyList(),
        )
    }

    val body = requestBody?.let { rb ->
        ServerRequestBodyDO(
            generatedName = "body",
            type = rb.type.toServerFieldType(ctx, required = rb.required),
            annotations = emptyList(),
        )
    }

    val success = responses
        ?.firstOrNull { it.statusCode in setOf(200, 201, 202, 204) }
        ?.let { r ->
            ServerSuccessResponseDO(
                rawResponse = r,
                type = r.type?.toServerFieldType(ctx, required = true),
            )
        }

    return ServerEndpointDO(
        rawOperation = this,
        generatedName = opName,
        params = params,
        requestBody = body,
        successResponse = success,
        annotations = emptyList(),
    )
}

private fun inferOperationId(method: RawPathDO.HttpMethodDO, path: String): String {
    val cleanPath = path.trim('/').replace("/", "_").replace("{", "").replace("}", "")
    return method.name.lowercase() + cleanPath.replaceFirstChar { it.uppercase() }
}

private fun String.toKotlinParamName(): String =
    replace("[^A-Za-z0-9_]".toRegex(), "_")
        .replace(Regex("_+"), "_")
        .trim('_')
        .ifBlank { "param" }
        .let { s ->
            val parts = s.split('_').filter { it.isNotBlank() }
            parts.first().lowercase() + parts.drop(1).joinToString("") { it.replaceFirstChar(Char::uppercase) }
        }

private fun String.toKotlinIdentifier(): String =
    replace("[^A-Za-z0-9_]".toRegex(), "_")
        .replace(Regex("_+"), "_")
        .trim('_')
        .ifBlank { "op" }
