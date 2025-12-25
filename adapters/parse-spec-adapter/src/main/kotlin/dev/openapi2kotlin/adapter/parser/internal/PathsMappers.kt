package dev.openapi2kotlin.adapter.parser.internal

import dev.openapi2kotlin.application.core.openapi2kotlin.model.raw.RawPathDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.raw.RawSchemaDO
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.parameters.Parameter
import io.swagger.v3.oas.models.parameters.RequestBody as OasRequestBody
import io.swagger.v3.oas.models.responses.ApiResponse as OasApiResponse

internal fun OpenAPI.toRawPaths(): List<RawPathDO> {
    // groupKey (first path segment) -> ops
    val groupKeyToOps = linkedMapOf<String, MutableList<RawPathDO.OperationDO>>()

    // groupKey -> tags (first entry will be groupKey, rest are collected op tags)
    val groupKeyToTags = linkedMapOf<String, LinkedHashSet<String>>()

    paths.orEmpty().forEach { (path, pathItem) ->
        val groupKey = path.toGroupKey()

        fun addOp(method: RawPathDO.HttpMethodDO, o: Operation?) {
            if (o == null) return

            val opTags: List<String> =
                o.tags.orEmpty().filter { it.isNotBlank() }

            // Ensure the first "tag" is always groupKey so downstream generatedApiName() is stable.
            val tagsSet = groupKeyToTags.getOrPut(groupKey) { linkedSetOf(groupKey) }
            tagsSet.addAll(opTags)

            val mergedParams = mergeParameters(
                pathParams = pathItem.parameters.orEmpty(),
                opParams = o.parameters.orEmpty(),
            )

            val params: List<RawPathDO.ParamDO> =
                mergedParams.mapNotNull { it.toParamDO() }

            val requestBody: RawPathDO.RequestBodyDO? =
                o.requestBody
                    ?.let { derefRequestBody(it) }
                    ?.toRequestBodyDO(this)

            val responses: List<RawPathDO.ResponseDO>? =
                o.responses
                    ?.entries
                    ?.mapNotNull { (code, resp) -> toResponseDO(code, resp) }
                    ?.sortedWith(compareBy<RawPathDO.ResponseDO> { it.statusCode })

            val op = RawPathDO.OperationDO(
                operationId = o.operationId,
                httpMethod = method,
                path = path,
                summary = o.summary,
                description = o.description,
                parameters = params,
                requestBody = requestBody,
                responses = responses,
            )

            groupKeyToOps.getOrPut(groupKey) { mutableListOf() }.add(op)
        }

        addOp(RawPathDO.HttpMethodDO.GET, pathItem.get)
        addOp(RawPathDO.HttpMethodDO.POST, pathItem.post)
        addOp(RawPathDO.HttpMethodDO.PUT, pathItem.put)
        addOp(RawPathDO.HttpMethodDO.PATCH, pathItem.patch)
        addOp(RawPathDO.HttpMethodDO.DELETE, pathItem.delete)
    }

    return groupKeyToOps.entries
        .map { (groupKey, ops) ->
            RawPathDO(
                tags = groupKeyToTags[groupKey]?.toList().orEmpty(),
                operations = ops.sortedWith(
                    compareBy<RawPathDO.OperationDO> { it.operationId ?: "" }
                        .thenBy { it.httpMethod.name }
                        .thenBy { it.path }
                ),
            )
        }
        .sortedWith(compareBy { it.tags.firstOrNull().orEmpty() }) // first tag is groupKey now
}

private fun String.toGroupKey(): String {
    val first = trim().trim('/').split('/').firstOrNull().orEmpty()
    val clean = first
        .replace("{", "")
        .replace("}", "")
        .trim()
    return clean.ifBlank { "default" }
}

private fun Parameter.toParamDO(): RawPathDO.ParamDO? {
    val location = when (`in`) {
        "path" -> RawPathDO.ParamLocationDO.PATH
        "query" -> RawPathDO.ParamLocationDO.QUERY
        "header" -> RawPathDO.ParamLocationDO.HEADER
        else -> return null
    }

    val required = (this.required == true) || (`in` == "path")
    val type = schema.toRawFieldType(required = required)

    return RawPathDO.ParamDO(
        name = name,
        location = location,
        required = required,
        type = type,
    )
}

private fun OasRequestBody.toRequestBodyDO(openApi: OpenAPI): RawPathDO.RequestBodyDO? {
    val mediaType = content?.get("application/json") ?: content?.values?.firstOrNull()
    val schema = mediaType?.schema ?: return null

    val required = this.required == true

    return RawPathDO.RequestBodyDO(
        required = required,
        type = schema.toRawFieldType(required = required),
    )
}

private fun OpenAPI.toResponseDO(code: String, raw: OasApiResponse): RawPathDO.ResponseDO? {
    val statusCode = code.toIntOrNull() ?: if (code == "default") DEFAULT_STATUS_CODE else return null
    val oasResponse = derefResponse(raw)

    val schema = oasResponse.content?.get("application/json")?.schema
        ?: oasResponse.content?.values?.firstOrNull()?.schema

    val type = schema?.toRawFieldType(required = true)

    return RawPathDO.ResponseDO(
        statusCode = statusCode,
        type = type,
    )
}

private fun Schema<*>?.toRawFieldType(required: Boolean): RawSchemaDO.RawFieldTypeDO {
    if (this == null) {
        return RawSchemaDO.RawPrimitiveTypeDO(
            type = RawSchemaDO.RawPrimitiveTypeDO.Type.OBJECT,
            format = null,
            nullable = true,
        )
    }

    val nullable = (nullable == true) || !required

    if (this is ArraySchema) {
        return RawSchemaDO.RawArrayTypeDO(
            elementType = items.toRawFieldType(required = true),
            nullable = nullable,
        )
    }

    `$ref`?.let { ref ->
        return RawSchemaDO.RawRefTypeDO(
            schemaName = ref.substringAfterLast('/'),
            nullable = nullable,
        )
    }

    val primitiveType = when (type) {
        "string" -> RawSchemaDO.RawPrimitiveTypeDO.Type.STRING
        "number" -> RawSchemaDO.RawPrimitiveTypeDO.Type.NUMBER
        "integer" -> RawSchemaDO.RawPrimitiveTypeDO.Type.INTEGER
        "boolean" -> RawSchemaDO.RawPrimitiveTypeDO.Type.BOOLEAN
        "object" -> RawSchemaDO.RawPrimitiveTypeDO.Type.OBJECT
        else -> RawSchemaDO.RawPrimitiveTypeDO.Type.OBJECT
    }

    return RawSchemaDO.RawPrimitiveTypeDO(
        type = primitiveType,
        format = format,
        nullable = nullable,
    )
}

private fun OpenAPI.derefResponse(response: OasApiResponse): OasApiResponse {
    val ref = response.`$ref` ?: return response
    val name = ref.substringAfterLast('/')
    return components?.responses?.get(name) ?: response
}

private fun OpenAPI.derefRequestBody(body: OasRequestBody): OasRequestBody {
    val ref = body.`$ref` ?: return body
    val name = ref.substringAfterLast('/')
    return components?.requestBodies?.get(name) ?: body
}

private fun mergeParameters(
    pathParams: List<Parameter>,
    opParams: List<Parameter>,
): List<Parameter> {
    val result = pathParams.toMutableList()
    opParams.forEach { opParam ->
        val idx = result.indexOfFirst { it.name == opParam.name && it.`in` == opParam.`in` }
        if (idx >= 0) result[idx] = opParam else result.add(opParam)
    }
    return result
}

private const val DEFAULT_STATUS_CODE: Int = -1
