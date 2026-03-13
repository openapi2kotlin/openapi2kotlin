package dev.openapi2kotlin.adapter.generateclient.restclient

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.UNIT
import dev.openapi2kotlin.application.core.openapi2kotlin.model.api.ApiEndpointDO
import dev.openapi2kotlin.tools.generatortools.TypeNameContext
import dev.openapi2kotlin.tools.generatortools.toTypeName

internal object RestClientApiPolicy {
    private val RESPONSE_ENTITY = ClassName("org.springframework.http", "ResponseEntity")
    private val RESPONSE_SPEC = ClassName("org.springframework.web.client.RestClient", "ResponseSpec")
    private val VOID = ClassName("java.lang", "Void")

    fun bodyReturnType(ep: ApiEndpointDO, ctx: TypeNameContext): TypeName =
        ep.successResponse?.type?.toTypeName(ctx) ?: UNIT

    fun httpInfoReturnType(ep: ApiEndpointDO, ctx: TypeNameContext): TypeName {
        val bodyType = ep.successResponse?.type?.toTypeName(ctx)
        val wrappedArg = bodyType ?: VOID
        return RESPONSE_ENTITY.parameterizedBy(wrappedArg)
    }

    fun responseSpecReturnType(): TypeName = RESPONSE_SPEC

    fun hasBody(ep: ApiEndpointDO): Boolean =
        ep.successResponse?.type != null
}
