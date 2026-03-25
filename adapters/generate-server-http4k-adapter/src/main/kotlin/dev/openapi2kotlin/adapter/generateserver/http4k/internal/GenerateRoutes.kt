package dev.openapi2kotlin.adapter.generateserver.http4k.internal

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterSpec
import dev.openapi2kotlin.application.core.openapi2kotlin.model.api.ApiDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.api.ApiEndpointDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.api.ApiParamDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.TrivialTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.raw.RawPathDO
import dev.openapi2kotlin.tools.generatortools.TypeNameContext
import dev.openapi2kotlin.tools.generatortools.toTypeName
import java.nio.file.Path

private val ROUTING_HTTP_HANDLER_T = ClassName("org.http4k.routing", "RoutingHttpHandler")
private val BODY_T = ClassName("org.http4k.core", "Body")

internal fun generateHttp4kRoutes(
    apis: List<ApiDO>,
    serverPackageName: String,
    outputDirPath: Path,
    ctx: TypeNameContext,
    basePath: String,
) {
    val outDir = outputDirPath.toFile()

    apis.forEach { api ->
        val apiStem = api.generatedName.removeSuffix("Api").ifBlank { api.generatedName }
        val routesFileName = apiStem + "Routes"
        val routesFunName = apiStem.replaceFirstChar { it.lowercaseChar() } + "Routes"

        FileSpec.builder(serverPackageName, routesFileName)
            .indent("    ")
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
    val methodRef = httpMethodRef(ep)
    val routePath = joinPaths(basePath, ep.rawOperation.path)
    builder.add("%S bind org.http4k.core.Method.%L to { request ->\n", routePath, methodRef)
    builder.indent()

    addParamReads(builder, ep)
    addRequestBodyRead(builder, ep, ctx)

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

private fun addParamReads(
    builder: CodeBlock.Builder,
    ep: ApiEndpointDO,
) {
    ep.params.forEach { param ->
        when (param.rawParam.location) {
            RawPathDO.ParamLocationDO.PATH -> addParamRead(builder, param, pathReadExpr(param))
            RawPathDO.ParamLocationDO.QUERY -> addParamRead(builder, param, queryReadExpr(param))
            RawPathDO.ParamLocationDO.HEADER -> addParamRead(builder, param, headerReadExpr(param))
        }
    }
}

private fun addParamRead(
    builder: CodeBlock.Builder,
    param: ApiParamDO,
    expr: CodeBlock,
) {
    builder.addStatement(
        "val %L = %L",
        param.generatedName,
        expr,
    )
}

private fun addRequestBodyRead(
    builder: CodeBlock.Builder,
    ep: ApiEndpointDO,
    ctx: TypeNameContext,
) {
    ep.requestBody?.let { body ->
        val bodyType = body.type.toTypeName(ctx)
        val isByteArray = (body.type as? TrivialTypeDO)?.kind == TrivialTypeDO.Kind.BYTE_ARRAY
        when {
            isByteArray && bodyType.isNullable ->
                builder.addStatement(
                    "val %L = request.bodyString().takeIf { it.isNotBlank() }?.encodeToByteArray()",
                    body.generatedName,
                )
            isByteArray ->
                builder.addStatement(
                    "val %L = request.bodyString().encodeToByteArray()",
                    body.generatedName,
                )
            bodyType.isNullable ->
                builder.addStatement(
                    "val %L = request.bodyString().takeIf { it.isNotBlank() }?.let { %T.auto<%T>().toLens()(request) }",
                    body.generatedName,
                    BODY_T,
                    bodyType.copy(nullable = false),
                )
            else ->
                builder.addStatement(
                    "val %L = %T.auto<%T>().toLens()(request)",
                    body.generatedName,
                    BODY_T,
                    bodyType,
                )
        }
    }
}
