package dev.openapi2kotlin.application.core.openapi2kotlin.model.raw

import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.FieldTypeDO

/**
 * Raw path data object representing an OpenAPI path. No business logic here - open api AS-IS ;)
 */
data class RawPathDO(
    val tag: String,
    val operations: List<ApiOperationDO>,
) {
    val interfaceName: String =
        tag.replace("[^A-Za-z0-9]".toRegex(), " ")  // non-alphanum → space
            .trim()
            .split(Regex("\\s+"))
            .joinToString("") { it.replaceFirstChar(Char::uppercase) } + "Api" // todo configurable

    data class ApiOperationDO(
        val operationId: String,
        val httpMethod: HttpMethodDO,
        val path: String,
        val summary: String?,
        val description: String?,
        val parameters: List<ApiParamDO>,
        val requestBody: ApiRequestDO?,
        val successResponse: ApiResponseDO?, // main 2xx response, nullable for 204/void
    )

    data class ApiParamDO (
        val name: String,
        val location: ApiParamLocationDO,
        val required: Boolean,
        val type: FieldTypeDO,
    )

    enum class ApiParamLocationDO {
        QUERY, PATH, HEADER,
    }

    data class ApiRequestDO(
        val required: Boolean,
        val type: FieldTypeDO,
    )

    data class ApiResponseDO(
        val statusCode: Int,
        val type: FieldTypeDO?, // null = no body (204, etc.)
    )

    enum class HttpMethodDO {
        GET, POST, PUT, PATCH, DELETE
    }
}