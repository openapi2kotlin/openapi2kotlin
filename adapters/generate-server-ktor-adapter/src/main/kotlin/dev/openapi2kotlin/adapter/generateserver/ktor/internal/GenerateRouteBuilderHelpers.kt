package dev.openapi2kotlin.adapter.generateserver.ktor.internal

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.MemberName
import dev.openapi2kotlin.application.core.openapi2kotlin.model.api.ApiEndpointDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.api.ApiParamDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.raw.RawPathDO
import dev.openapi2kotlin.tools.generatortools.TypeNameContext
import dev.openapi2kotlin.tools.generatortools.toTypeName

internal fun routeMember(ep: ApiEndpointDO): MemberName =
    when (ep.rawOperation.httpMethod) {
        RawPathDO.HttpMethodDO.GET -> M_get
        RawPathDO.HttpMethodDO.POST -> M_post
        RawPathDO.HttpMethodDO.PUT -> M_put
        RawPathDO.HttpMethodDO.PATCH -> M_patch
        RawPathDO.HttpMethodDO.DELETE -> M_delete
    }

internal fun routeLabel(ep: ApiEndpointDO): String =
    when (ep.rawOperation.httpMethod) {
        RawPathDO.HttpMethodDO.GET -> "get"
        RawPathDO.HttpMethodDO.POST -> "post"
        RawPathDO.HttpMethodDO.PUT -> "put"
        RawPathDO.HttpMethodDO.PATCH -> "patch"
        RawPathDO.HttpMethodDO.DELETE -> "delete"
    }

internal fun statusMemberName(statusCode: Int): String? =
    when (statusCode) {
        HTTP_STATUS_OK -> "OK"
        HTTP_STATUS_CREATED -> "Created"
        HTTP_STATUS_ACCEPTED -> "Accepted"
        HTTP_STATUS_NO_CONTENT -> "NoContent"
        else -> null
    }

internal fun CodeBlock.Builder.addParamReads(
    ep: ApiEndpointDO,
    label: String,
) {
    ep.params.forEach { param ->
        addStatement("val %L = %L", param.generatedName, paramReadExpr(param, label))
    }
}

internal fun CodeBlock.Builder.addRequestBodyRead(
    ep: ApiEndpointDO,
    ctx: TypeNameContext,
) {
    ep.requestBody?.let { body ->
        val bodyType = body.type.toTypeName(ctx)
        val receiveMember =
            if (bodyType.isNullable) M_receiveNullable else M_receive
        val receiveType =
            if (bodyType.isNullable) bodyType.copy(nullable = false) else bodyType

        addStatement("val %L = call.%M<%T>()", body.generatedName, receiveMember, receiveType)
    }
}

private fun paramReadExpr(
    param: ApiParamDO,
    label: String,
): CodeBlock =
    when (param.rawParam.location) {
        RawPathDO.ParamLocationDO.PATH -> pathReadExpr(param, label)
        RawPathDO.ParamLocationDO.QUERY -> queryReadExpr(param)
        RawPathDO.ParamLocationDO.HEADER -> headerReadExpr(param)
    }
