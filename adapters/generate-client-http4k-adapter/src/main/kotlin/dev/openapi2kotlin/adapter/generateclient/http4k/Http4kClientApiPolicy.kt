package dev.openapi2kotlin.adapter.generateclient.http4k

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.UNIT
import dev.openapi2kotlin.application.core.openapi2kotlin.model.api.ApiEndpointDO
import dev.openapi2kotlin.tools.generatortools.TypeNameContext
import dev.openapi2kotlin.tools.generatortools.toTypeName

internal object Http4kClientApiPolicy {
    private val RESPONSE = ClassName("org.http4k.core", "Response")

    fun bodyReturnType(
        ep: ApiEndpointDO,
        ctx: TypeNameContext,
    ): TypeName = ep.successResponse?.type?.toTypeName(ctx) ?: UNIT

    fun httpInfoReturnType(): TypeName = RESPONSE

    fun hasBody(ep: ApiEndpointDO): Boolean = ep.successResponse?.type != null
}
