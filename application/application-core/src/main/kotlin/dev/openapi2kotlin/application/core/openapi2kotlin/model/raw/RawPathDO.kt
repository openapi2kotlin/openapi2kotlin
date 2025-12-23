package dev.openapi2kotlin.application.core.openapi2kotlin.model.raw

/**
 * Raw path data object representing an OpenAPI path. No business logic here - open api AS-IS ;)
 */
data class RawPathDO(
    val tags: List<String>,
    val operations: List<OperationDO>,
) {
    data class OperationDO(
        val operationId: String?,
        val httpMethod: HttpMethodDO,
        val path: String,
        val summary: String?,
        val description: String?,
        val parameters: List<ParamDO>,
        val requestBody: RequestBodyDO?,
        val responses: List<ResponseDO>?,
    )

    data class ParamDO(
        val name: String,
        val location: ParamLocationDO,
        val required: Boolean,
        val type: RawSchemaDO.RawFieldTypeDO,
    )

    enum class ParamLocationDO { QUERY, PATH, HEADER }

    data class RequestBodyDO(
        val required: Boolean,
        val type: RawSchemaDO.RawFieldTypeDO,
    )

    data class ResponseDO(
        val statusCode: Int,
        val type: RawSchemaDO.RawFieldTypeDO?, // null = no body (204, etc.)
    )

    enum class HttpMethodDO { GET, POST, PUT, PATCH, DELETE }
}