package dev.openapi2kotlin.adapter.generateserver.http4k.internal

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterSpec
import dev.openapi2kotlin.application.core.openapi2kotlin.model.api.ApiDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.api.ApiEndpointDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.ListTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.ModelDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.TrivialTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.raw.RawPathDO
import dev.openapi2kotlin.tools.generatortools.TypeNameContext
import dev.openapi2kotlin.tools.generatortools.toTypeName
import java.nio.file.Path

private val ROUTING_HTTP_HANDLER_T = ClassName("org.http4k.routing", "RoutingHttpHandler")
private val RESPONSE_T = ClassName("org.http4k.core", "Response")
private val STATUS_T = ClassName("org.http4k.core", "Status")
private val BODY_T = ClassName("org.http4k.core", "Body")

internal fun generateHttp4kRoutes(
    apis: List<ApiDO>,
    serverPackageName: String,
    modelPackageName: String,
    outputDirPath: Path,
    models: List<ModelDO>,
    basePath: String,
) {
    val outDir = outputDirPath.toFile()
    val bySchemaName: Map<String, ModelDO> = models.associateBy { it.rawSchema.originalName }
    val ctx = TypeNameContext(modelPackageName = modelPackageName, bySchemaName = bySchemaName)

    apis.forEach { api ->
        val apiStem = api.generatedName.removeSuffix("Api").ifBlank { api.generatedName }
        val routesFileName = apiStem + "Routes"
        val routesFunName = apiStem.replaceFirstChar { it.lowercaseChar() } + "Routes"

        FileSpec.builder(serverPackageName, routesFileName)
            .addImport("org.http4k.format.KotlinxSerialization", "auto")
            .addImport("org.http4k.routing", "bind", "path", "routes")
            .addTypeAliasIfNeeded()
            .addFunction(generateRoutes(api, basePath, routesFunName, serverPackageName, ctx))
            .build()
            .writeTo(outDir)
    }
}

private fun FileSpec.Builder.addTypeAliasIfNeeded(): FileSpec.Builder = this

private fun generateRoutes(
    api: ApiDO,
    basePath: String,
    routesFunName: String,
    serverPackageName: String,
    ctx: TypeNameContext,
): FunSpec {
    val apiType = ClassName(serverPackageName, api.generatedName)

    return FunSpec.builder(routesFunName)
        .addParameter(ParameterSpec.builder("api", apiType).build())
        .returns(ROUTING_HTTP_HANDLER_T)
        .addCode(buildRoutesCode(api, basePath, ctx))
        .build()
}

private fun buildRoutesCode(
    api: ApiDO,
    basePath: String,
    ctx: TypeNameContext,
): CodeBlock {
    val builder = CodeBlock.builder()
    builder.add("return routes(\n")
    builder.indent()
    api.endpoints.forEachIndexed { index, ep ->
        builder.add(buildRouteEntry(ep, basePath, ctx))
        if (index != api.endpoints.lastIndex) builder.add(",\n") else builder.add("\n")
    }
    builder.unindent()
    builder.add(")\n")
    return builder.build()
}

private fun buildRouteEntry(
    ep: ApiEndpointDO,
    basePath: String,
    ctx: TypeNameContext,
): CodeBlock {
    val builder = CodeBlock.builder()
    val methodRef = when (ep.rawOperation.httpMethod) {
        RawPathDO.HttpMethodDO.GET -> "GET"
        RawPathDO.HttpMethodDO.POST -> "POST"
        RawPathDO.HttpMethodDO.PUT -> "PUT"
        RawPathDO.HttpMethodDO.PATCH -> "PATCH"
        RawPathDO.HttpMethodDO.DELETE -> "DELETE"
    }
    val routePath = joinPaths(basePath, ep.rawOperation.path)
    builder.add("%S bind org.http4k.core.Method.%L to { request ->\n", routePath, methodRef)
    builder.indent()

    ep.params.forEach { param ->
        when (param.rawParam.location) {
            RawPathDO.ParamLocationDO.PATH -> builder.addStatement("val %L = %L", param.generatedName, pathReadExpr(param))
            RawPathDO.ParamLocationDO.QUERY -> builder.addStatement("val %L = %L", param.generatedName, queryReadExpr(param))
            RawPathDO.ParamLocationDO.HEADER -> builder.addStatement("val %L = %L", param.generatedName, headerReadExpr(param))
        }
    }

    ep.requestBody?.let { body ->
        val bodyType = body.type.toTypeName(ctx)
        val isByteArray = (body.type as? TrivialTypeDO)?.kind == TrivialTypeDO.Kind.BYTE_ARRAY
        when {
            isByteArray && bodyType.isNullable -> builder.addStatement("val %L = request.bodyString().takeIf { it.isNotBlank() }?.encodeToByteArray()", body.generatedName)
            isByteArray -> builder.addStatement("val %L = request.bodyString().encodeToByteArray()", body.generatedName)
            bodyType.isNullable -> builder.addStatement(
                "val %L = request.bodyString().takeIf { it.isNotBlank() }?.let { %T.auto<%T>().toLens()(request) }",
                body.generatedName,
                BODY_T,
                bodyType.copy(nullable = false),
            )
            else -> builder.addStatement(
                "val %L = %T.auto<%T>().toLens()(request)",
                body.generatedName,
                BODY_T,
                bodyType,
            )
        }
    }

    builder.addStatement("return@to api.%LWithHttpInfo(%L)", ep.generatedName, methodArgs(ep))
    builder.unindent()
    builder.add("}")
    return builder.build()
}

private fun methodArgs(ep: ApiEndpointDO): String =
    buildList {
        ep.params.forEach { add(it.generatedName) }
        ep.requestBody?.let { add(it.generatedName) }
    }.joinToString(", ")

private fun joinPaths(basePath: String, endpointPath: String): String {
    val base = basePath.trim().trim('/').takeIf { it.isNotBlank() }
    val path = endpointPath.trim().trim('/').takeIf { it.isNotBlank() }
    return when {
        base == null && path == null -> "/"
        base == null -> "/${path}"
        path == null -> "/${base}"
        else -> "/${base}/${path}"
    }
}

private fun pathReadExpr(param: dev.openapi2kotlin.application.core.openapi2kotlin.model.api.ApiParamDO): CodeBlock {
    val key = param.rawParam.name
    return when ((param.type as? TrivialTypeDO)?.kind) {
        TrivialTypeDO.Kind.STRING, null -> CodeBlock.of("request.path(%S) ?: return@to %T(%T.BAD_REQUEST)", key, RESPONSE_T, STATUS_T)
        TrivialTypeDO.Kind.LONG -> CodeBlock.of("request.path(%S)?.toLongOrNull() ?: return@to %T(%T.BAD_REQUEST)", key, RESPONSE_T, STATUS_T)
        TrivialTypeDO.Kind.INT -> CodeBlock.of("request.path(%S)?.toIntOrNull() ?: return@to %T(%T.BAD_REQUEST)", key, RESPONSE_T, STATUS_T)
        TrivialTypeDO.Kind.FLOAT, TrivialTypeDO.Kind.DOUBLE -> CodeBlock.of("request.path(%S)?.toDoubleOrNull() ?: return@to %T(%T.BAD_REQUEST)", key, RESPONSE_T, STATUS_T)
        TrivialTypeDO.Kind.BOOLEAN -> CodeBlock.of("request.path(%S)?.toBooleanStrictOrNull() ?: return@to %T(%T.BAD_REQUEST)", key, RESPONSE_T, STATUS_T)
        else -> CodeBlock.of("request.path(%S) ?: return@to %T(%T.BAD_REQUEST)", key, RESPONSE_T, STATUS_T)
    }
}

private fun queryReadExpr(param: dev.openapi2kotlin.application.core.openapi2kotlin.model.api.ApiParamDO): CodeBlock {
    val key = param.rawParam.name
    return when (param.type) {
        is ListTypeDO -> {
            val kind = ((param.type as ListTypeDO).elementType as? TrivialTypeDO)?.kind
            when (kind) {
                TrivialTypeDO.Kind.LONG -> CodeBlock.of("request.queries(%S).mapNotNull(String::toLongOrNull).takeIf { it.isNotEmpty() }", key)
                TrivialTypeDO.Kind.INT -> CodeBlock.of("request.queries(%S).mapNotNull(String::toIntOrNull).takeIf { it.isNotEmpty() }", key)
                TrivialTypeDO.Kind.FLOAT, TrivialTypeDO.Kind.DOUBLE -> CodeBlock.of("request.queries(%S).mapNotNull(String::toDoubleOrNull).takeIf { it.isNotEmpty() }", key)
                TrivialTypeDO.Kind.BOOLEAN -> CodeBlock.of("request.queries(%S).mapNotNull(String::toBooleanStrictOrNull).takeIf { it.isNotEmpty() }", key)
                else -> CodeBlock.of("request.queries(%S).filterNotNull().takeIf { it.isNotEmpty() }", key)
            }
        }
        else -> when ((param.type as? TrivialTypeDO)?.kind) {
            TrivialTypeDO.Kind.STRING, null -> CodeBlock.of("request.query(%S)", key)
            TrivialTypeDO.Kind.LONG -> CodeBlock.of("request.query(%S)?.toLongOrNull()", key)
            TrivialTypeDO.Kind.INT -> CodeBlock.of("request.query(%S)?.toIntOrNull()", key)
            TrivialTypeDO.Kind.FLOAT, TrivialTypeDO.Kind.DOUBLE -> CodeBlock.of("request.query(%S)?.toDoubleOrNull()", key)
            TrivialTypeDO.Kind.BOOLEAN -> CodeBlock.of("request.query(%S)?.toBooleanStrictOrNull()", key)
            else -> CodeBlock.of("request.query(%S)", key)
        }
    }
}

private fun headerReadExpr(param: dev.openapi2kotlin.application.core.openapi2kotlin.model.api.ApiParamDO): CodeBlock {
    val key = param.rawParam.name
    return when ((param.type as? TrivialTypeDO)?.kind) {
        TrivialTypeDO.Kind.STRING, null -> CodeBlock.of("request.header(%S)", key)
        TrivialTypeDO.Kind.LONG -> CodeBlock.of("request.header(%S)?.toLongOrNull()", key)
        TrivialTypeDO.Kind.INT -> CodeBlock.of("request.header(%S)?.toIntOrNull()", key)
        TrivialTypeDO.Kind.FLOAT, TrivialTypeDO.Kind.DOUBLE -> CodeBlock.of("request.header(%S)?.toDoubleOrNull()", key)
        TrivialTypeDO.Kind.BOOLEAN -> CodeBlock.of("request.header(%S)?.toBooleanStrictOrNull()", key)
        else -> CodeBlock.of("request.header(%S)", key)
    }
}
