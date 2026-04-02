package dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.api.helpers

import dev.openapi2kotlin.application.core.openapi2kotlin.domain.api.ApiAnnotationDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.api.ApiDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.api.ApiEndpointDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.raw.RawPathDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.raw.RawSchemaDO
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
private const val HTTP_OK = 200
private const val HTTP_CREATED = 201
private const val HTTP_ACCEPTED = 202
private const val HTTP_NO_CONTENT = 204
private const val HTTP_BAD_REQUEST = 400
private const val HTTP_UNAUTHORIZED = 401
private const val HTTP_FORBIDDEN = 403
private const val HTTP_NOT_FOUND = 404
private const val HTTP_CONFLICT = 409
private const val HTTP_INTERNAL_SERVER_ERROR = 500
private const val HTTP_NOT_IMPLEMENTED = 501
private const val HTTP_SERVICE_UNAVAILABLE = 503

internal fun List<ApiDO>.handleServerSwaggerAnnotations(
    cfg: OpenApi2KotlinUseCase.ApiConfig?,
    ctx: ApisContext,
) {
    val serverCfg = cfg as? OpenApi2KotlinUseCase.ApiConfig.Server ?: return
    if (!serverCfg.swagger) return

    forEach { api ->
        val tags = api.rawPath.tags.distinct().filter { it.isNotBlank() }
        if (tags.isNotEmpty()) {
            api.annotations = api.annotations +
                tags.map { tag ->
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

    val args =
        buildList {
            op.summary
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?.let { add("summary = ${it.toKotlinStringLiteral()}") }
            op.operationId
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?.let { add("operationId = ${it.toKotlinStringLiteral()}") }
            op.description
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?.let { add("description = ${it.toKotlinStringLiteral()}") }

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

        val args =
            buildList {
                add("responseCode = ${renderSwaggerResponseCode(r.statusCode).toKotlinStringLiteral()}")
                add("description = ${desc.toKotlinStringLiteral()}")
                if (contentCode != null) add("content = [$contentCode]")
            }.joinToString(", ")

        "$API_RESPONSE($args)"
    }
}

private fun buildContentCode(
    type: RawSchemaDO.RawFieldTypeDO,
    ctx: ApisContext,
): String? =
    when (type) {
        is RawSchemaDO.RawRefTypeDO -> buildRefContentCode(type.schemaName, ctx)
        is RawSchemaDO.RawArrayTypeDO -> buildArrayContentCode(type, ctx)
        is RawSchemaDO.RawPrimitiveTypeDO -> null
    }

private fun buildRefContentCode(
    schemaName: String,
    ctx: ApisContext,
): String? {
    val model = ctx.modelsBySchemaName[schemaName] ?: return null
    val fqcn = "${model.packageName}.${model.generatedName}"
    return "$CONTENT(schema = $SCHEMA(implementation = $fqcn::class))"
}

private fun buildArrayContentCode(
    type: RawSchemaDO.RawArrayTypeDO,
    ctx: ApisContext,
): String? {
    val element = type.elementType as? RawSchemaDO.RawRefTypeDO
    val model = element?.let { ctx.modelsBySchemaName[it.schemaName] }
    return model?.let {
        val fqcn = "${it.packageName}.${it.generatedName}"
        "$CONTENT(array = $ARRAY_SCHEMA(schema = $SCHEMA(implementation = $fqcn::class)))"
    }
}

private fun buildParameterAnnotation(raw: RawPathDO.ParamDO): ApiAnnotationDO {
    val args =
        buildList {
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

    val args =
        buildList {
            add("required = ${rb.required}")
            if (content != null) add("content = [$content]")
        }

    return ApiAnnotationDO(
        fqName = SWAGGER_REQUEST_BODY,
        argsCode = args,
    )
}

private fun RawPathDO.ParamLocationDO.toSwaggerParameterInEnum(): String =
    when (this) {
        RawPathDO.ParamLocationDO.QUERY -> "QUERY"
        RawPathDO.ParamLocationDO.PATH -> "PATH"
        RawPathDO.ParamLocationDO.HEADER -> "HEADER"
    }

private fun defaultDescriptionForCode(code: Int): String =
    when (code) {
        HTTP_OK -> "Success"
        HTTP_CREATED -> "Created"
        HTTP_ACCEPTED -> "Accepted"
        HTTP_NO_CONTENT -> "No Content"
        HTTP_BAD_REQUEST -> "Bad Request"
        HTTP_UNAUTHORIZED -> "Unauthorized"
        HTTP_FORBIDDEN -> "Forbidden"
        HTTP_NOT_FOUND -> "Not Found"
        HTTP_CONFLICT -> "Conflict"
        HTTP_INTERNAL_SERVER_ERROR -> "Internal Server Error"
        HTTP_NOT_IMPLEMENTED -> "Not Implemented"
        HTTP_SERVICE_UNAVAILABLE -> "Service Unavailable"
        else -> "Response"
    }
