package dev.openapi2kotlin.tools.apigenerator

import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.UNIT
import dev.openapi2kotlin.application.core.openapi2kotlin.model.api.ApiEndpointDO
import dev.openapi2kotlin.tools.generatortools.TypeNameContext
import dev.openapi2kotlin.tools.generatortools.toTypeName

interface ApiPolicy {
    val suspendFunctions: Boolean

    fun returnType(ep: ApiEndpointDO, ctx: TypeNameContext): TypeName

    object Default : ApiPolicy {
        override val suspendFunctions: Boolean = true

        override fun returnType(ep: ApiEndpointDO, ctx: TypeNameContext): TypeName =
            ep.successResponse?.type?.toTypeName(ctx) ?: UNIT
    }
}