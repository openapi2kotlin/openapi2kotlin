package dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.api.helpers

import dev.openapi2kotlin.application.core.openapi2kotlin.model.api.ApiAnnotationDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.api.ApiDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.api.ApiEndpointDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.raw.RawPathDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.raw.RawSchemaDO
import dev.openapi2kotlin.application.usecase.openapi2kotlin.OpenApi2KotlinUseCase

private const val TAG = "io.swagger.v3.oas.annotations.tags.Tag"
private const val OPERATION = "io.swagger.v3.oas.annotations.Operation"
private const val API_RESPONSE = "io.swagger.v3.oas.annotations.responses.ApiResponse"
private const val CONTENT = "io.swagger.v3.oas.annotations.media.Content"
private const val SCHEMA = "io.swagger.v3.oas.annotations.media.Schema"
private const val ARRAY_SCHEMA = "io.swagger.v3.oas.annotations.media.ArraySchema"

private const val PARAMETER = "io.swagger.v3.oas.annotations.Parameter"
private const val PARAMETER_IN = "io.swagger.v3.oas.annotations.enums.ParameterIn"

private const val SWAGGER_REQUEST_BODY = "io.swagger.v3.oas.annotations.parameters.RequestBody"

internal fun List<ApiDO>.handleServerSwaggerAnnotations(
    cfg: OpenApi2KotlinUseCase.ApiConfig?,
    ctx: ApisContext,
) {
    val serverCfg = cfg as? OpenApi2KotlinUseCase.ApiConfig.Server ?: return
    if (!serverCfg.swagger.enabled) return

    forEach { api ->
        val tags = api.rawPath.tags.distinct().filter { it.isNotBlank() }
        if (tags.isNotEmpty()) {
            api.annotations = api.annotations + tags.map { tag ->
                ApiAnnotationDO(
                    fqName = TAG,
                    argsCode = listOf("name = ${tag.toKotlinStringLiteral()}"),
                )
            }
        }

        api.endpoints.forEach { ep ->
            ep.annotations = ep.annotations + buildOperationAnnotation(ep, api.rawPath.tags, ctx)
            ep.params.forEach { p -> p.annotations = p.annotations + buildParameterAnnotation(p.rawParam) }
            ep.requestBody?.let { rb ->
                rb.annotations = rb.annotations + buildRequestBodyAnnotation(ep.rawOperation, ctx)
            }
        }
    }
}

private fun buildOperationAnnotation(
    ep: ApiEndpointDO,
    apiTags: List<String>,
    ctx: ApisContext,
): ApiAnnotationDO {
    val op = ep.rawOperation
    val tags = apiTags.distinct().filter { it.isNotBlank() }

    val args = buildList {
        op.summary?.trim()?.takeIf { it.isNotBlank() }?.let { add("summary = ${it.toKotlinStringLiteral()}") }
        op.operationId?.trim()?.takeIf { it.isNotBlank() }?.let { add("operationId = ${it.toKotlinStringLiteral()}") }
        op.description?.trim()?.takeIf { it.isNotBlank() }?.let { add("description = ${it.toKotlinStringLiteral()}") }

        if (tags.isNotEmpty()) {
            add("tags = [${tags.joinToString(", ") { it.toKotlinStringLiteral() }}]")
        }

        val responsesCode = buildApiResponsesCode(op.responses.orEmpty(), ctx)
        if (responsesCode.isNotEmpty()) {
            add("responses = [$responsesCode]")
        }
    }

    return ApiAnnotationDO(
        fqName = OPERATION,
        argsCode = args,
    )
}

private fun buildApiResponsesCode(
    responses: List<RawPathDO.ResponseDO>,
    ctx: ApisContext,
): String {
    if (responses.isEmpty()) return ""

    return responses.joinToString(
        separator = ",\n      ",
        prefix = "\n      ",
        postfix = "\n    ",
    ) { r ->
        val desc = defaultDescriptionForCode(r.statusCode)
        val contentCode = r.type?.let { buildContentCode(it, ctx) }

        val args = buildList {
            add("responseCode = ${r.statusCode.toString().toKotlinStringLiteral()}")
            add("description = ${desc.toKotlinStringLiteral()}")
            if (contentCode != null) add("content = [$contentCode]")
        }.joinToString(", ")

        "$API_RESPONSE($args)"
    }
}

private fun buildContentCode(
    type: RawSchemaDO.RawFieldTypeDO,
    ctx: ApisContext,
): String? {
    return when (type) {
        is RawSchemaDO.RawRefTypeDO -> {
            val model = ctx.modelsBySchemaName[type.schemaName] ?: return null
            val fqcn = "${model.packageName}.${model.generatedName}"
            "$CONTENT(schema = $SCHEMA(implementation = $fqcn::class))"
        }

        is RawSchemaDO.RawArrayTypeDO -> {
            val element = type.elementType
            if (element is RawSchemaDO.RawRefTypeDO) {
                val model = ctx.modelsBySchemaName[element.schemaName] ?: return null
                val fqcn = "${model.packageName}.${model.generatedName}"
                "$CONTENT(array = $ARRAY_SCHEMA(schema = $SCHEMA(implementation = $fqcn::class)))"
            } else {
                null
            }
        }

        is RawSchemaDO.RawPrimitiveTypeDO -> null
    }
}

private fun buildParameterAnnotation(raw: RawPathDO.ParamDO): ApiAnnotationDO {
    val args = buildList {
        add("name = ${raw.name.toKotlinStringLiteral()}")
        add("required = ${raw.required}")
        add("`in` = $PARAMETER_IN.${raw.location.toSwaggerParameterInEnum()}")
    }

    return ApiAnnotationDO(
        fqName = PARAMETER,
        argsCode = args,
    )
}

private fun buildRequestBodyAnnotation(
    op: RawPathDO.OperationDO,
    ctx: ApisContext,
): ApiAnnotationDO {
    val rb = op.requestBody ?: return ApiAnnotationDO(SWAGGER_REQUEST_BODY, emptyList())

    val content = buildContentCode(rb.type, ctx)

    val args = buildList {
        add("required = ${rb.required}")
        if (content != null) add("content = [$content]")
    }

    return ApiAnnotationDO(
        fqName = SWAGGER_REQUEST_BODY,
        argsCode = args,
    )
}

private fun RawPathDO.ParamLocationDO.toSwaggerParameterInEnum(): String = when (this) {
    RawPathDO.ParamLocationDO.QUERY -> "QUERY"
    RawPathDO.ParamLocationDO.PATH -> "PATH"
    RawPathDO.ParamLocationDO.HEADER -> "HEADER"
}

private fun defaultDescriptionForCode(code: Int): String = when (code) {
    200 -> "Success"
    201 -> "Created"
    202 -> "Accepted"
    204 -> "No Content"
    400 -> "Bad Request"
    401 -> "Unauthorized"
    403 -> "Forbidden"
    404 -> "Not Found"
    409 -> "Conflict"
    500 -> "Internal Server Error"
    501 -> "Not Implemented"
    503 -> "Service Unavailable"
    else -> "Response"
}

private fun String.toKotlinStringLiteral(): String =
    buildString {
        append('"')
        for (ch in this@toKotlinStringLiteral) {
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
