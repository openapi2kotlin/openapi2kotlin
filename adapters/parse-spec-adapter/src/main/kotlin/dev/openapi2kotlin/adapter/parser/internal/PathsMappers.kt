package dev.openapi2kotlin.adapter.parser.internal

import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.ListTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.PrimitiveTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.RefTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.FieldTypeDO
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

            val tags = o.tags ?: listOf("default")
            val tag = tags.firstOrNull().orEmpty().ifBlank { "default" }

            val allParams: List<Parameter> = mergeParameters(
                pathItem.parameters.orEmpty(),
                o.parameters.orEmpty(),
            )

            val parameters = allParams.mapNotNull { p ->
                val loc = when (p.`in`) {
                    "path" -> RawPathDO.ApiParamLocationDO.PATH
                    "query" -> RawPathDO.ApiParamLocationDO.QUERY
                    "header" -> RawPathDO.ApiParamLocationDO.HEADER
                    else -> return@mapNotNull null // ignore cookie/unsupported for now
                }

                val schema = p.schema
                val required = p.required == true

                val dtoType = schemaToDtoTypeForParam(schema, required)

                RawPathDO.ApiParamDO(
                    name = p.name,
                    location = loc,
                    required = required,
                    type = dtoType,
                )
            }

            // ----- request body (with $ref support) -----
            val requestBody = o.requestBody?.let { raw ->
                val rb = this.derefRequestBody(raw)

                val mt = rb.content?.get("application/json")
                    ?: rb.content?.values?.firstOrNull()

                val schema = mt?.schema
                if (schema == null) null
                else RawPathDO.ApiRequestDO(
                    required = rb.required == true,
                    type = schemaToDtoTypeForParam(schema, required = rb.required == true),
                )
            }

            val successResponse = this.findSuccessResponse(o)

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

/* ---------- helpers ---------- */

private fun mergeParameters(
    pathParams: List<Parameter>,
    opParams: List<Parameter>,
): List<Parameter> {
    // operation-level overrides path-level with same (name, in)
    val result = mutableListOf<Parameter>()
    result.addAll(pathParams)
    opParams.forEach { opParam ->
        val idx = result.indexOfFirst { it.name == opParam.name && it.`in` == opParam.`in` }
        if (idx >= 0) result[idx] = opParam else result.add(opParam)
    }
    return result
}

/**
 * Map a Swagger schema to our internal DtoType for parameters / bodies.
 */
private fun schemaToDtoTypeForParam(
    schema: Schema<*>?,
    required: Boolean,
): FieldTypeDO {
    if (schema == null) {
        return PrimitiveTypeDO(PrimitiveTypeDO.PrimitiveTypeNameDO.ANY, nullable = true)
    }

    val nullableFromRequired = !required

    if (schema is ArraySchema) {
        val elementType = schemaToDtoTypeForParam(
            schema = schema.items,
            required = true,
        )
        val nullable = schema.nullable == true || nullableFromRequired
        return ListTypeDO(
            elementType = elementType,
            nullable = nullable,
        )
    }

    schema.`$ref`?.let { ref ->
        val name = ref.substringAfterLast('/')
        val nullable = schema.nullable == true || nullableFromRequired
        return RefTypeDO(
            schemaName = name,
            nullable = nullable,
        )
    }

    val kind: PrimitiveTypeDO.PrimitiveTypeNameDO = when (schema.type) {
        "string" -> PrimitiveTypeDO.PrimitiveTypeNameDO.STRING
        "integer" -> if (schema.format == "int64") PrimitiveTypeDO.PrimitiveTypeNameDO.LONG else PrimitiveTypeDO.PrimitiveTypeNameDO.INT
        "number" -> PrimitiveTypeDO.PrimitiveTypeNameDO.DOUBLE
        "boolean" -> PrimitiveTypeDO.PrimitiveTypeNameDO.BOOLEAN
        else -> PrimitiveTypeDO.PrimitiveTypeNameDO.ANY
    }

    val nullable = schema.nullable == true || nullableFromRequired
    return PrimitiveTypeDO(kind, nullable)
}

/**
 * Pick "main" 2xx response and map to DtoType.
 */
private fun OpenAPI.findSuccessResponse(
    operation: Operation,
): RawPathDO.ApiResponseDO? {
    val responses = operation.responses ?: return null

    val preferredCodes = listOf("200", "201", "202", "204")
    val code = preferredCodes.firstOrNull { responses.containsKey(it) } ?: return null

    val oasResponse = this.derefResponse(responses[code] ?: return null)

    // try application/json or first content
    val schema = oasResponse.content?.get("application/json")?.schema
        ?: oasResponse.content?.values?.firstOrNull()?.schema

    val dtoType = schema?.let { schemaToDtoTypeForParam(it, required = true) }

    return RawPathDO.ApiResponseDO(
        statusCode = code.toInt(),
        type = dtoType, // null for 204 or no-schema
    )
}

/**
 * Resolve $ref on responses.
 */
private fun OpenAPI.derefResponse(
    response: OasApiResponse,
): OasApiResponse {
    val ref = response.`$ref` ?: return response
    val name = ref.substringAfterLast('/')
    return this.components?.responses?.get(name) ?: response
}

/**
 * Resolve $ref on requestBodies.
 */
private fun OpenAPI.derefRequestBody(
    body: OasRequestBody,
): OasRequestBody {
    val ref = body.`$ref` ?: return body
    val name = ref.substringAfterLast('/')
    return this.components?.requestBodies?.get(name) ?: body
}

private fun inferOperationId(method: RawPathDO.HttpMethodDO, path: String): String {
    // fallback if operationId missing
    val cleanPath = path.trim('/').replace("/", "_").replace("{", "").replace("}", "")
    return method.name.lowercase() + cleanPath.replaceFirstChar { it.uppercase() }
}
