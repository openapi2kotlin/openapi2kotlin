package dev.openapi2kotlin.adapter.parser.internal

import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.FieldTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.ListTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.RefTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.TrivialTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.raw.RawPathDO
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.parameters.Parameter
import io.swagger.v3.oas.models.parameters.RequestBody as OasRequestBody
import io.swagger.v3.oas.models.responses.ApiResponse as OasApiResponse

internal fun OpenAPI.toRawPaths(): List<RawPathDO> {
    val tagToOps = linkedMapOf<String, MutableList<RawPathDO.ApiOperationDO>>()

    this.paths.orEmpty().forEach { (path, pathItem) ->
        fun addOp(method: RawPathDO.HttpMethodDO, o: Operation?) {
            if (o == null) return

            val tag = (o.tags?.firstOrNull().orEmpty().ifBlank { "default" })

            val allParams = mergeParameters(
                pathItem.parameters.orEmpty(),
                o.parameters.orEmpty(),
            )

            val parameters = allParams.mapNotNull { p -> p.toApiParamDO() }

            val requestBody = o.requestBody
                ?.let { derefRequestBody(it) }
                ?.toApiRequestDO(this)

            val successResponse = findSuccessResponse(o)

            val op = RawPathDO.ApiOperationDO(
                operationId = o.operationId ?: inferOperationId(method, path),
                httpMethod = method,
                path = path,
                summary = o.summary,
                description = o.description,
                parameters = parameters,
                requestBody = requestBody,
                successResponse = successResponse,
            )

            tagToOps.getOrPut(tag) { mutableListOf() }.add(op)
        }

        addOp(RawPathDO.HttpMethodDO.GET, pathItem.get)
        addOp(RawPathDO.HttpMethodDO.POST, pathItem.post)
        addOp(RawPathDO.HttpMethodDO.PUT, pathItem.put)
        addOp(RawPathDO.HttpMethodDO.PATCH, pathItem.patch)
        addOp(RawPathDO.HttpMethodDO.DELETE, pathItem.delete)
    }

    return tagToOps.map { (tag, ops) ->
        RawPathDO(
            tag = tag,
            operations = ops.sortedBy { it.operationId },
        )
    }.sortedBy { it.tag }
}

private fun Parameter.toApiParamDO(): RawPathDO.ApiParamDO? {
    val loc = when (this.`in`) {
        "path" -> RawPathDO.ApiParamLocationDO.PATH
        "query" -> RawPathDO.ApiParamLocationDO.QUERY
        "header" -> RawPathDO.ApiParamLocationDO.HEADER
        else -> return null
    }

    val required = this.required == true
    val type = schemaToFieldType(this.schema, required)

    return RawPathDO.ApiParamDO(
        name = this.name,
        location = loc,
        required = required,
        type = type,
    )
}

private fun OasRequestBody.toApiRequestDO(openApi: OpenAPI): RawPathDO.ApiRequestDO? {
    val mt = this.content?.get("application/json") ?: this.content?.values?.firstOrNull()
    val schema = mt?.schema ?: return null

    val required = this.required == true

    return RawPathDO.ApiRequestDO(
        required = required,
        type = schemaToFieldType(schema, required),
    )
}

private fun schemaToFieldType(
    schema: Schema<*>?,
    required: Boolean,
): FieldTypeDO {
    if (schema == null) {
        return TrivialTypeDO(TrivialTypeDO.Kind.ANY, nullable = true)
    }

    val nullableFromRequired = !required

    if (schema is ArraySchema) {
        val elementType = schemaToFieldType(schema.items, required = true)
        val nullable = schema.nullable == true || nullableFromRequired
        return ListTypeDO(elementType = elementType, nullable = nullable)
    }

    schema.`$ref`?.let { ref ->
        val name = ref.substringAfterLast('/')
        val nullable = schema.nullable == true || nullableFromRequired
        return RefTypeDO(schemaName = name, nullable = nullable)
    }

    val nullable = schema.nullable == true || nullableFromRequired

    return when (schema.type) {
        "string" -> {
            val kind = when (schema.format) {
                "date" -> TrivialTypeDO.Kind.LOCAL_DATE
                "date-time" -> TrivialTypeDO.Kind.OFFSET_DATE_TIME
                "byte" -> TrivialTypeDO.Kind.BYTE_ARRAY
                "binary" -> TrivialTypeDO.Kind.BYTE_ARRAY
                else -> TrivialTypeDO.Kind.STRING
            }
            TrivialTypeDO(kind = kind, nullable = nullable)
        }

        "integer" -> TrivialTypeDO(
            kind = when (schema.format) {
                "int64" -> TrivialTypeDO.Kind.LONG
                else -> TrivialTypeDO.Kind.INT
            },
            nullable = nullable,
        )

        "number" -> TrivialTypeDO(
            kind = when (schema.format) {
                "float" -> TrivialTypeDO.Kind.FLOAT
                "double" -> TrivialTypeDO.Kind.DOUBLE
                else -> TrivialTypeDO.Kind.DOUBLE
            },
            nullable = nullable,
        )

        "boolean" -> TrivialTypeDO(TrivialTypeDO.Kind.BOOLEAN, nullable = nullable)

        "object" -> TrivialTypeDO(TrivialTypeDO.Kind.ANY, nullable = nullable)

        else -> TrivialTypeDO(TrivialTypeDO.Kind.ANY, nullable = nullable)
    }
}

private fun OpenAPI.findSuccessResponse(operation: Operation): RawPathDO.ApiResponseDO? {
    val responses = operation.responses ?: return null

    val preferredCodes = listOf("200", "201", "202", "204")
    val code = preferredCodes.firstOrNull { responses.containsKey(it) } ?: return null

    val oasResponse = derefResponse(responses[code] ?: return null)

    val schema = oasResponse.content?.get("application/json")?.schema
        ?: oasResponse.content?.values?.firstOrNull()?.schema

    val type = schema?.let { schemaToFieldType(it, required = true) }

    return RawPathDO.ApiResponseDO(
        statusCode = code.toInt(),
        type = type,
    )
}

private fun OpenAPI.derefResponse(response: OasApiResponse): OasApiResponse {
    val ref = response.`$ref` ?: return response
    val name = ref.substringAfterLast('/')
    return this.components?.responses?.get(name) ?: response
}

private fun OpenAPI.derefRequestBody(body: OasRequestBody): OasRequestBody {
    val ref = body.`$ref` ?: return body
    val name = ref.substringAfterLast('/')
    return this.components?.requestBodies?.get(name) ?: body
}

private fun mergeParameters(pathParams: List<Parameter>, opParams: List<Parameter>): List<Parameter> {
    val result = mutableListOf<Parameter>()
    result.addAll(pathParams)

    opParams.forEach { opParam ->
        val idx = result.indexOfFirst { it.name == opParam.name && it.`in` == opParam.`in` }
        if (idx >= 0) result[idx] = opParam else result.add(opParam)
    }

    return result
}

private fun inferOperationId(method: RawPathDO.HttpMethodDO, path: String): String {
    val cleanPath = path.trim('/').replace("/", "_").replace("{", "").replace("}", "")
    return method.name.lowercase() + cleanPath.replaceFirstChar { it.uppercase() }
}
