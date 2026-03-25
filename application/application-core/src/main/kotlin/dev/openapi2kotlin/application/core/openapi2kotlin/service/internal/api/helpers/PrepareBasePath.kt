package dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.api.helpers

import dev.openapi2kotlin.application.core.openapi2kotlin.model.raw.RawServerDO
import dev.openapi2kotlin.application.usecase.openapi2kotlin.OpenApi2KotlinUseCase
import io.github.oshai.kotlinlogging.KotlinLogging

private val log = KotlinLogging.logger {}

internal fun prepareBasePath(
    rawServers: List<RawServerDO>,
    config: OpenApi2KotlinUseCase.Config,
): String {
    val basePathVarName = config.api!!.basePathVar.trim()
    return rawServers.resolveBasePathFromVariable(basePathVarName)
        ?: rawServers.resolveBasePathFromRelativeUrl()
        ?: emptyBasePath()
}

/**
 * Extracts the "/path" portion from a server URL-like string.
 * Works with templates (e.g. https://{tenant}.host/{basePath}) without URI parsing.
 */
private fun extractPathFromUrlLike(url: String): String {
    val u = url.trim()
    val schemeIdx = u.indexOf("://")
    return when {
        schemeIdx >= 0 -> {
            val afterScheme = u.substring(schemeIdx + SCHEME_SEPARATOR_LENGTH)
            val slashIdx = afterScheme.indexOf('/')
            slashIdx.substringOrEmpty(afterScheme)
        }
        u.startsWith("/") -> u
        else -> {
            val slashIdx = u.indexOf('/')
            slashIdx.substringOrEmpty(u)
        }
    }
}

private fun isRelativeUrlLike(url: String): Boolean {
    val trimmed = url.trim()
    return trimmed.startsWith("/")
}

private fun resolvePathOrEmpty(
    urlTemplate: String,
    substitutedUrl: String,
    basePathVarName: String,
): String {
    val pathTemplate = extractPathFromUrlLike(substitutedUrl)
    if (pathTemplate.isBlank() || pathTemplate == "/") return ""

    val resolved = pathTemplate.normalizePath()

    if (resolved.contains("{") && resolved.contains("}")) {
        log.warn {
            "basePath contains unresolved placeholders after applying '$basePathVarName'. " +
                "url='$urlTemplate', substitutedUrl='$substitutedUrl', resolved='$resolved'."
        }
    }

    return resolved
}

private fun String.normalizePath(): String {
    val trimmed = trim()
    val collapsed = trimmed.replace(Regex("/+"), "/")
    val noTrailing = if (collapsed.length > 1) collapsed.trimEnd('/') else collapsed
    if (noTrailing.isBlank()) return ""
    return if (noTrailing.startsWith("/")) noTrailing else "/$noTrailing"
}

private fun List<RawServerDO>.resolveBasePathFromVariable(basePathVarName: String): String? {
    if (basePathVarName.isBlank()) return null

    val server =
        asSequence()
            .filter { !it.url.isNullOrBlank() }
            .firstOrNull { candidate ->
                candidate.vars.orEmpty().any { it.name == basePathVarName && it.defaultValue.isNotBlank() }
            }

    return if (server == null) {
        log.debug {
            "No OpenAPI server variable named '$basePathVarName' with a default value was found; " +
                "basePath defaults to empty."
        }
        ""
    } else {
        val urlTemplate = server.url.orEmpty().trim()
        val basePathVarValue =
            server.vars
                .orEmpty()
                .firstOrNull { it.name == basePathVarName }
                ?.defaultValue
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                .orEmpty()
        val substitutedUrl = urlTemplate.replace("{$basePathVarName}", basePathVarValue)

        resolvePathOrEmpty(
            urlTemplate = urlTemplate,
            substitutedUrl = substitutedUrl,
            basePathVarName = basePathVarName,
        )
    }
}

private fun List<RawServerDO>.resolveBasePathFromRelativeUrl(): String? {
    val relativeServerUrl =
        asSequence()
            .mapNotNull { it.url?.trim()?.takeIf(String::isNotBlank) }
            .firstOrNull { isRelativeUrlLike(it) }

    return relativeServerUrl?.let {
        resolvePathOrEmpty(
            urlTemplate = it,
            substitutedUrl = it,
            basePathVarName = "",
        )
    }
}

private const val SCHEME_SEPARATOR_LENGTH = 3

private fun Int.substringOrEmpty(source: String): String = if (this >= 0) source.substring(this) else ""

private fun emptyBasePath(): String {
    log.debug { "No relative OpenAPI server URL found and basePathVar is blank; basePath defaults to empty." }
    return ""
}
