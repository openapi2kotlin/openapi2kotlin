package dev.openapi2kotlin.adapter.generateserver.ktor.internal

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.MemberName
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.api.ApiDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.api.ApiEndpointDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.model.ModelDO
import dev.openapi2kotlin.tools.generatortools.TypeNameContext
import java.nio.file.Path

internal val ROUTE_T = ClassName("io.ktor.server.routing", "Route")
internal val HTTP_STATUS_T = ClassName("io.ktor.http", "HttpStatusCode")
internal val M_route = MemberName("io.ktor.server.routing", "route")
internal val M_get = MemberName("io.ktor.server.routing", "get")
internal val M_post = MemberName("io.ktor.server.routing", "post")
internal val M_put = MemberName("io.ktor.server.routing", "put")
internal val M_patch = MemberName("io.ktor.server.routing", "patch")
internal val M_delete = MemberName("io.ktor.server.routing", "delete")
internal val M_receive = MemberName("io.ktor.server.request", "receive")
internal val M_receiveNullable = MemberName("io.ktor.server.request", "receiveNullable")
internal val M_respond = MemberName("io.ktor.server.response", "respond")
internal const val HTTP_STATUS_OK = 200
internal const val HTTP_STATUS_CREATED = 201
internal const val HTTP_STATUS_ACCEPTED = 202
internal const val HTTP_STATUS_NO_CONTENT = 204

internal fun generateRoutes(
    apis: List<ApiDO>,
    serverPackageName: String,
    modelPackageName: String,
    outputDirPath: Path,
    models: List<ModelDO>,
) {
    val outDir = outputDirPath.toFile()
    val bySchemaName: Map<String, ModelDO> = models.associateBy { it.rawSchema.originalName }
    val ctx = TypeNameContext(modelPackageName = modelPackageName, bySchemaName = bySchemaName)

    apis.forEach { api ->
        // Ktor routing is adapter-specific: infer basePath + generate a Route.() extension function.
        val basePath = inferBasePath(api)

        val apiStem = api.generatedName.removeSuffix("Api").ifBlank { api.generatedName }
        val routesFileName = apiStem + "Routes"
        val routesFunName = apiStem.replaceFirstChar { it.lowercaseChar() } + "Routes"

        FileSpec
            .builder(serverPackageName, routesFileName)
            .indent("    ")
            .addFunction(
                generateRoutes(
                    api = api,
                    basePath = basePath,
                    routesFunName = routesFunName,
                    serverPackageName = serverPackageName,
                    ctx = ctx,
                ),
            ).build()
            .writeTo(outDir)
    }
}

private fun inferBasePath(api: ApiDO): String {
    // Best-effort: choose the shortest concrete path and take its first segment.
    val shortest =
        api.rawPath.operations
            .map { it.path.trim() }
            .filter { it.isNotBlank() }
            .minByOrNull { it.length }
            ?: "/"

    val seg =
        shortest
            .trim('/')
            .split('/')
            .firstOrNull()
            .orEmpty()
    return "/" + seg
}

private fun generateRoutes(
    api: ApiDO,
    basePath: String,
    routesFunName: String,
    serverPackageName: String,
    ctx: TypeNameContext,
): FunSpec {
    val apiType = ClassName(serverPackageName, api.generatedName)

    return FunSpec
        .builder(routesFunName)
        .receiver(ROUTE_T)
        .addParameter("api", apiType)
        .addCode(
            CodeBlock
                .builder()
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
                }.unindent()
                .addStatement("}")
                .build(),
        ).build()
}

private fun suffixUnderBase(
    base: String,
    full: String,
): String {
    if (!full.startsWith(base)) return full
    val rem = full.removePrefix(base)
    return if (rem.isBlank()) "" else rem
}

private fun CodeBlock.Builder.addKtorHandler(
    ep: ApiEndpointDO,
    ctx: TypeNameContext,
) {
    val methodMember = routeMember(ep)
    val label = routeLabel(ep)

    addStatement("%M {", methodMember)
    indent()
    addParamReads(ep, label)
    addRequestBodyRead(ep, ctx)

    val args =
        buildString {
            ep.params.forEachIndexed { idx, p ->
                if (idx > 0) append(", ")
                append(p.generatedName)
            }
            ep.requestBody?.let {
                if (ep.params.isNotEmpty()) append(", ")
                append(it.generatedName)
            }
        }

    val statusCode = ep.successResponse?.rawResponse?.statusCode ?: HTTP_STATUS_OK
    val returnsBody = statusCode != HTTP_STATUS_NO_CONTENT && ep.successResponse?.type != null

    if (returnsBody) {
        addStatement("val result = api.%L(%L)", ep.generatedName, args)
    } else {
        addStatement("api.%L(%L)", ep.generatedName, args)
    }

    val statusMember = statusMemberName(statusCode)

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
