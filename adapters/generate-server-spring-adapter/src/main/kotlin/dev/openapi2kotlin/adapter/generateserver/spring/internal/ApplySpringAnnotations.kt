package dev.openapi2kotlin.adapter.generateserver.spring.internal

import dev.openapi2kotlin.application.core.openapi2kotlin.model.api.ApiAnnotationDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.api.ApiContextDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.api.ApiEndpointDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.api.ApiParamDO
import dev.openapi2kotlin.application.core.openapi2kotlin.model.raw.RawPathDO

fun ApiContextDO.applySpringMvcAnnotations() {
    val hasBasePath = basePath.isNotBlank()

    apis.forEach { api ->
        val apiAnnotations = mutableListOf(
            ann("org.springframework.web.bind.annotation.RestController"),
            ann("org.springframework.validation.annotation.Validated"),
        )

        if (hasBasePath) {
            apiAnnotations += ann("org.springframework.web.bind.annotation.RequestMapping") {
                addLiteral("""value = [${basePath.quoted()}]""")
            }
        }

        api.annotations = api.annotations + apiAnnotations

        api.endpoints.forEach { ep ->
            ep.annotations = ep.annotations + ep.springComposedMapping(
                basePath = if (hasBasePath) basePath else null
            )

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

private fun ApiEndpointDO.springComposedMapping(basePath: String?): ApiAnnotationDO {
    val endpointPath = if (basePath != null) {
        rawOperation.path.toRelativeTo(basePath)
    } else {
        rawOperation.path.normalizePathForSpring()
    }

    val fqAnn = when (rawOperation.httpMethod) {
        RawPathDO.HttpMethodDO.GET -> "org.springframework.web.bind.annotation.GetMapping"
        RawPathDO.HttpMethodDO.POST -> "org.springframework.web.bind.annotation.PostMapping"
        RawPathDO.HttpMethodDO.PUT -> "org.springframework.web.bind.annotation.PutMapping"
        RawPathDO.HttpMethodDO.PATCH -> "org.springframework.web.bind.annotation.PatchMapping"
        RawPathDO.HttpMethodDO.DELETE -> "org.springframework.web.bind.annotation.DeleteMapping"
    }

    return ann(fqAnn) {
        addLiteral("""value = [${endpointPath.quoted()}]""")
    }
}

private fun String.toRelativeTo(basePath: String): String {
    val b = basePath.normalizePathForSpring()
    val full = this.normalizePathForSpring()

    if (b == "/") return full
    if (full == b) return "/"
    if (full.startsWith("$b/")) return full.removePrefix(b)
    return full // base is not a prefix; fall back to absolute
}

private fun String.normalizePathForSpring(): String {
    val t = trim()
    if (t.isBlank() || t == "/") return "/"
    val collapsed = t.replace(Regex("/+"), "/").trim('/')
    return "/$collapsed"
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
