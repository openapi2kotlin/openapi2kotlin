import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.MemberName
import dev.openapi2kotlin.adapter.tools.TypeNameContext
import dev.openapi2kotlin.adapter.tools.toTypeName
import dev.openapi2kotlin.application.core.openapi2kotlin.model.api.ApiDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.api.ApiEndpointDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.api.ApiParamDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.TrivialTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.raw.RawPathDO

private val ROUTE_T = ClassName("io.ktor.server.routing", "Route")
private val HTTP_STATUS_T = ClassName("io.ktor.http", "HttpStatusCode")
private val M_route = MemberName("io.ktor.server.routing", "route")
private val M_get = MemberName("io.ktor.server.routing", "get")
private val M_post = MemberName("io.ktor.server.routing", "post")
private val M_put = MemberName("io.ktor.server.routing", "put")
private val M_patch = MemberName("io.ktor.server.routing", "patch")
private val M_delete = MemberName("io.ktor.server.routing", "delete")
private val M_receive = MemberName("io.ktor.server.request", "receive")
private val M_respond = MemberName("io.ktor.server.response", "respond")

internal fun buildKtorRoute(
    api: ApiDO,
    basePath: String,
    routesFunName: String,
    serverPackageName: String,
    ctx: TypeNameContext,
): FunSpec {
    val apiType = ClassName(serverPackageName, api.generatedName)

    return FunSpec.builder(routesFunName)
        .receiver(ROUTE_T)
        .addParameter("api", apiType)
        .addCode(
            CodeBlock.builder()
                .addStatement("%M(%S) {", M_route, basePath)
                .indent()
                .apply {
                    // Split endpoints into: base (exact basePath) and id (basePath + "/{id}") and others.
                    val baseOps = api.endpoints.filter { it.rawOperation.path == basePath }
                    val idOps = api.endpoints.filter { it.rawOperation.path == basePath + "/{id}" }
                    val otherOps = api.endpoints - baseOps.toSet() - idOps.toSet()

                    baseOps.forEach { addKtorHandler(it, ctx) }

                    if (idOps.isNotEmpty()) {
                        addStatement("%M(%S) {", M_route, "/{id}")
                        indent()
                        idOps.forEach { addKtorHandler(it, ctx) }
                        unindent()
                        addStatement("}")
                    }

                    // Fallback for remaining paths: generate nested routes under base using route("...").
                    otherOps
                        .groupBy { suffixUnderBase(basePath, it.rawOperation.path) }
                        .forEach { (suffix, eps) ->
                            if (suffix.isNotBlank()) {
                                addStatement("%M(%S) {", M_route, suffix)
                                indent()
                            }
                            eps.forEach { addKtorHandler(it, ctx) }
                            if (suffix.isNotBlank()) {
                                unindent()
                                addStatement("}")
                            }
                        }
                }
                .unindent()
                .addStatement("}")
                .build()
        )
        .build()
}

private fun suffixUnderBase(base: String, full: String): String {
    if (!full.startsWith(base)) return full
    val rem = full.removePrefix(base)
    return if (rem.isBlank()) "" else rem
}

private fun CodeBlock.Builder.addKtorHandler(
    ep: ApiEndpointDO,
    ctx: TypeNameContext,
) {
    val methodMember = when (ep.rawOperation.httpMethod) {
        RawPathDO.HttpMethodDO.GET -> M_get
        RawPathDO.HttpMethodDO.POST -> M_post
        RawPathDO.HttpMethodDO.PUT -> M_put
        RawPathDO.HttpMethodDO.PATCH -> M_patch
        RawPathDO.HttpMethodDO.DELETE -> M_delete
    }

    val label = when (ep.rawOperation.httpMethod) {
        RawPathDO.HttpMethodDO.GET -> "get"
        RawPathDO.HttpMethodDO.POST -> "post"
        RawPathDO.HttpMethodDO.PUT -> "put"
        RawPathDO.HttpMethodDO.PATCH -> "patch"
        RawPathDO.HttpMethodDO.DELETE -> "delete"
    }

    addStatement("%M {", methodMember)
    indent()

    ep.params.forEach { p ->
        when (p.rawParam.location) {
            RawPathDO.ParamLocationDO.PATH -> {
                addStatement(
                    "val %L = call.parameters[%S] ?: return@%L call.%M(%T.BadRequest)",
                    p.generatedName,
                    p.rawParam.name,
                    label,
                    M_respond,
                    HTTP_STATUS_T,
                )
            }

            RawPathDO.ParamLocationDO.QUERY -> {
                addStatement("val %L = %L", p.generatedName, queryReadExpr(p))
            }

            RawPathDO.ParamLocationDO.HEADER -> {
                addStatement("val %L = %L", p.generatedName, headerReadExpr(p))
            }
        }
    }

    ep.requestBody?.let { body ->
        val bodyType = body.type.toTypeName(ctx)
        addStatement("val %L = call.%M<%T>()", body.generatedName, M_receive, bodyType)
    }

    val args = buildString {
        ep.params.forEachIndexed { idx, p ->
            if (idx > 0) append(", ")
            append(p.generatedName)
        }
        ep.requestBody?.let {
            if (ep.params.isNotEmpty()) append(", ")
            append(it.generatedName)
        }
    }

    val statusCode = ep.successResponse?.rawResponse?.statusCode ?: 200
    val returnsBody = statusCode != 204 && ep.successResponse?.type != null

    if (returnsBody) {
        addStatement("val result = api.%L(%L)", ep.generatedName, args)
    } else {
        addStatement("api.%L(%L)", ep.generatedName, args)
    }

    val statusMember = when (statusCode) {
        200 -> "OK"
        201 -> "Created"
        202 -> "Accepted"
        204 -> "NoContent"
        else -> null
    }

    if (statusMember != null) {
        if (returnsBody) {
            addStatement("call.%M(%T.%L, result)", M_respond, HTTP_STATUS_T, statusMember)
        } else {
            addStatement("call.%M(%T.%L)", M_respond, HTTP_STATUS_T, statusMember)
        }
    } else {
        if (returnsBody) {
            addStatement("call.%M(%T.fromValue(%L), result)", M_respond, HTTP_STATUS_T, statusCode)
        } else {
            addStatement("call.%M(%T.fromValue(%L))", M_respond, HTTP_STATUS_T, statusCode)
        }
    }

    unindent()
    addStatement("}")
}

private fun queryReadExpr(p: ApiParamDO): CodeBlock {
    val key = p.rawParam.name
    return when (p.type) {
        is TrivialTypeDO -> {
            val t = p.type as TrivialTypeDO
            when (t.kind) {
                TrivialTypeDO.Kind.STRING -> CodeBlock.of("call.request.queryParameters[%S]", key)
                TrivialTypeDO.Kind.LONG -> CodeBlock.of("call.request.queryParameters[%S]?.toLong()", key)
                TrivialTypeDO.Kind.INT -> CodeBlock.of("call.request.queryParameters[%S]?.toInt()", key)
                TrivialTypeDO.Kind.FLOAT,
                TrivialTypeDO.Kind.DOUBLE -> CodeBlock.of("call.request.queryParameters[%S]?.toDouble()", key)
                TrivialTypeDO.Kind.BOOLEAN -> CodeBlock.of("call.request.queryParameters[%S]?.toBoolean()", key)
                else -> CodeBlock.of("call.request.queryParameters[%S]", key)
            }
        }

        else -> CodeBlock.of("call.request.queryParameters[%S]", key)
    }
}

private fun headerReadExpr(p: ApiParamDO): CodeBlock {
    val key = p.rawParam.name
    return when (p.type) {
        is TrivialTypeDO -> {
            val t = p.type as TrivialTypeDO
            when (t.kind) {
                TrivialTypeDO.Kind.STRING -> CodeBlock.of("call.request.headers[%S]", key)
                TrivialTypeDO.Kind.LONG -> CodeBlock.of("call.request.headers[%S]?.toLong()", key)
                TrivialTypeDO.Kind.INT -> CodeBlock.of("call.request.headers[%S]?.toInt()", key)
                TrivialTypeDO.Kind.FLOAT,
                TrivialTypeDO.Kind.DOUBLE -> CodeBlock.of("call.request.headers[%S]?.toDouble()", key)
                TrivialTypeDO.Kind.BOOLEAN -> CodeBlock.of("call.request.headers[%S]?.toBoolean()", key)
                else -> CodeBlock.of("call.request.headers[%S]", key)
            }
        }

        else -> CodeBlock.of("call.request.headers[%S]", key)
    }
}
