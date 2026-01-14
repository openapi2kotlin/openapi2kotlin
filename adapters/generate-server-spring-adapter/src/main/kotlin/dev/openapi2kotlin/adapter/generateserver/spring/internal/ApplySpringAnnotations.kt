package dev.openapi2kotlin.adapter.generateserver.spring.internal

import dev.openapi2kotlin.application.core.openapi2kotlin.model.api.ApiAnnotationDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.api.ApiDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.api.ApiEndpointDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.api.ApiParamDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.raw.RawPathDO

fun List<ApiDO>.applySpringMvcAnnotations() {
    forEach { api ->
        api.annotations = api.annotations + listOf(
            ann("org.springframework.web.bind.annotation.RestController"),
            ann("org.springframework.validation.annotation.Validated"),
            ann("org.springframework.web.bind.annotation.RequestMapping") {
                addLiteral("""value = [${api.inferBasePath().quoted()}]""")
            },
        )

        api.endpoints.forEach { ep ->
            ep.annotations = ep.annotations + ep.springRequestMapping()

            ep.params.forEach { p ->
                p.annotations = p.annotations + p.springParamAnnotation()
            }

            ep.requestBody?.let { body ->
                body.annotations = body.annotations + listOf(
                    ann("org.springframework.web.bind.annotation.RequestBody"),
                    ann("jakarta.validation.Valid"),
                )
            }
        }
    }
}

private fun ApiDO.inferBasePath(): String {
    val paths = endpoints.map { it.rawOperation.path }.distinct().sorted()
    if (paths.isEmpty()) return "/"

    fun split(p: String) = p.trim().trim('/').split('/').filter { it.isNotBlank() }

    val first = split(paths.first())
    val last = split(paths.last())

    val common = buildList {
        val n = minOf(first.size, last.size)
        for (i in 0 until n) {
            if (first[i] == last[i]) add(first[i]) else break
        }
    }

    return "/" + common.joinToString("/")
}

private fun ApiEndpointDO.springRequestMapping(): ApiAnnotationDO {
    val method = when (rawOperation.httpMethod) {
        RawPathDO.HttpMethodDO.GET -> "RequestMethod.GET"
        RawPathDO.HttpMethodDO.POST -> "RequestMethod.POST"
        RawPathDO.HttpMethodDO.PUT -> "RequestMethod.PUT"
        RawPathDO.HttpMethodDO.PATCH -> "RequestMethod.PATCH"
        RawPathDO.HttpMethodDO.DELETE -> "RequestMethod.DELETE"
    }

    return ann("org.springframework.web.bind.annotation.RequestMapping") {
        addLiteral("""value = [${rawOperation.path.quoted()}]""")
        addLiteral("""method = [$method]""")
    }
}

private fun ApiParamDO.springParamAnnotation(): ApiAnnotationDO =
    when (rawParam.location) {
        RawPathDO.ParamLocationDO.PATH ->
            ann("org.springframework.web.bind.annotation.PathVariable") {
                addLiteral("""value = ${rawParam.name.quoted()}""")
                addLiteral("""required = true""")
            }

        RawPathDO.ParamLocationDO.QUERY ->
            ann("org.springframework.web.bind.annotation.RequestParam") {
                addLiteral("""value = ${rawParam.name.quoted()}""")
                addLiteral("""required = ${rawParam.required}""")
            }

        RawPathDO.ParamLocationDO.HEADER ->
            ann("org.springframework.web.bind.annotation.RequestHeader") {
                addLiteral("""value = ${rawParam.name.quoted()}""")
                addLiteral("""required = ${rawParam.required}""")
            }
    }

private fun ann(
    fqName: String,
    buildArgs: MutableList<String>.() -> Unit = {},
): ApiAnnotationDO {
    val args = mutableListOf<String>()
    args.buildArgs()
    return ApiAnnotationDO(
        fqName = fqName,
        argsCode = args,
    )
}

private fun MutableList<String>.addLiteral(code: String) {
    add(code)
}

private fun String.quoted(): String =
    "\"" + this
        .replace("\\", "\\\\")
        .replace("\"", "\\\"") + "\""
