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
    require(basePathVarName.isNotBlank()) { "api.basePathVar must not be blank" }

    val server = rawServers
        .asSequence()
        .filter { !it.url.isNullOrBlank() }
        .firstOrNull { s ->
            s.vars.orEmpty().any { it.name == basePathVarName && it.defaultValue.isNotBlank() }
        }
        ?: rawServers.firstOrNull { !it.url.isNullOrBlank() }

    if (server == null) {
        log.debug { "No OpenAPI servers found; basePath defaults to empty." }
        return ""
    }

    val urlTemplate = server.url.orEmpty().trim()

    val basePathVarValue =
        server.vars
            .orEmpty()
            .firstOrNull { it.name == basePathVarName }
            ?.defaultValue
            ?.trim()
            ?.takeIf { it.isNotBlank() }

    // Substitute basePath var first (works for both absolute and relative server URLs)
    val substitutedUrl = urlTemplate.replace("{${basePathVarName}}", basePathVarValue.orEmpty())

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

/**
 * Extracts the "/path" portion from a server URL-like string.
 * Works with templates (e.g. https://{tenant}.host/{basePath}) without URI parsing.
 */
private fun extractPathFromUrlLike(url: String): String {
    val u = url.trim()
    val schemeIdx = u.indexOf("://")
    if (schemeIdx >= 0) {
        val afterScheme = u.substring(schemeIdx + 3)
        val slashIdx = afterScheme.indexOf('/')
        return if (slashIdx >= 0) afterScheme.substring(slashIdx) else ""
    }

    // relative path-like
    if (u.startsWith("/")) return u

    // host-like without scheme: "example.com/v2"
    val slashIdx = u.indexOf('/')
    return if (slashIdx >= 0) u.substring(slashIdx) else ""
}


private fun String.normalizePath(): String {
    val trimmed = trim()
    val collapsed = trimmed.replace(Regex("/+"), "/")
    val noTrailing = if (collapsed.length > 1) collapsed.trimEnd('/') else collapsed
    if (noTrailing.isBlank()) return ""
    return if (noTrailing.startsWith("/")) noTrailing else "/$noTrailing"
}