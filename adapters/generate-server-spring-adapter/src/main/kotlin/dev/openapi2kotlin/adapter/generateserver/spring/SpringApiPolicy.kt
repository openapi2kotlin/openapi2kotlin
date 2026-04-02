package dev.openapi2kotlin.adapter.generateserver.spring

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.api.ApiEndpointDO
import dev.openapi2kotlin.tools.apigenerator.ApiPolicy
import dev.openapi2kotlin.tools.generatortools.TypeNameContext
import dev.openapi2kotlin.tools.generatortools.toTypeName

object SpringApiPolicy : ApiPolicy {
    override val suspendFunctions: Boolean = false

    private val RESPONSE_ENTITY = ClassName("org.springframework.http", "ResponseEntity")
    private val VOID = ClassName("java.lang", "Void")
    private const val HTTP_STATUS_OK = 200
    private const val HTTP_STATUS_NO_CONTENT = 204

    override fun returnType(
        ep: ApiEndpointDO,
        ctx: TypeNameContext,
    ): TypeName {
        val status = ep.successResponse?.rawResponse?.statusCode ?: HTTP_STATUS_OK
        val bodyType = ep.successResponse?.type?.toTypeName(ctx)

        val wrappedArg =
            if (status == HTTP_STATUS_NO_CONTENT || bodyType == null) VOID else bodyType

        return RESPONSE_ENTITY.parameterizedBy(wrappedArg)
    }
}
