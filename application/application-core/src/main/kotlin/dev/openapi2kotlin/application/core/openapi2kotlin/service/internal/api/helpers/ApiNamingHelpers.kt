package dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.api.helpers

import dev.openapi2kotlin.application.core.openapi2kotlin.model.raw.RawSchemaDO

private const val PLURAL_IES_SUFFIX_MIN_LENGTH = 3
private const val TRAILING_S_MIN_LENGTH = 1
private const val IES_SUFFIX_LENGTH = 3
private const val SES_TO_S_DROP_LENGTH = 2

internal fun String.looksLikeCollectionEndpoint(): Boolean {
    val segments = trim('/').split('/').filter { it.isNotBlank() }
    val lastSegment = segments.lastOrNull() ?: return false
    return !lastSegment.isPathParam() && lastSegment.isLikelyPlural()
}

internal fun String.resourceNameSegments(
    verb: OperationVerb,
    successResponseType: RawSchemaDO.RawFieldTypeDO?,
    singularized: Boolean,
    pluralized: Boolean,
): List<String> {
    val segments = trim('/').split('/').filter { it.isNotBlank() }
    val staticSegments = segments.filterNot { it.isPathParam() }
    val lastStatic = staticSegments.lastOrNull()

    return if (segments.isEmpty() || lastStatic == null) {
        emptyList()
    } else {
        val lastIsParam = segments.last().isPathParam()
        val previousStatic = staticSegments.dropLast(1).lastOrNull()
        val isCollection = successResponseType is RawSchemaDO.RawArrayTypeDO || looksLikeCollectionEndpoint()

        when (verb) {
            OperationVerb.LIST -> listOf(if (pluralized) lastStatic.pluralize() else lastStatic)
            OperationVerb.RETRIEVE,
            OperationVerb.UPDATE,
            OperationVerb.DELETE,
            OperationVerb.PATCH,
            ->
                listOf(
                    selectedResourceSegment(
                        lastIsParam = lastIsParam,
                        previousStatic = previousStatic,
                        lastStatic = lastStatic,
                    ).transformSingularized(singularized),
                )
            OperationVerb.CREATE ->
                createResourceNameSegments(
                    lastIsParam = lastIsParam,
                    previousStatic = previousStatic,
                    lastStatic = lastStatic,
                    singularized = singularized,
                    isCollection = isCollection,
                )
        }
    }
}

private fun createResourceNameSegments(
    lastIsParam: Boolean,
    previousStatic: String?,
    lastStatic: String,
    singularized: Boolean,
    isCollection: Boolean,
): List<String> =
    when {
        lastIsParam && previousStatic != null ->
            listOf(
                previousStatic.transformSingularized(singularized),
                lastStatic.transformSingularized(singularized),
            )
        isCollection -> listOf(lastStatic.transformSingularized(singularized))
        else -> listOf(lastStatic.transformSingularized(singularized))
    }

private fun selectedResourceSegment(
    lastIsParam: Boolean,
    previousStatic: String?,
    lastStatic: String,
): String = if (lastIsParam) previousStatic ?: lastStatic else lastStatic

private fun String.isPathParam(): Boolean = startsWith("{") && endsWith("}")

private fun String.isLikelyPlural(): Boolean {
    val normalized = lowercase()
    return normalized.endsWith("s") && !normalized.endsWith("ss")
}

private fun String.singularize(): String {
    val normalized = trim()
    return when {
        normalized.endsWith("ies", ignoreCase = true) && normalized.length > PLURAL_IES_SUFFIX_MIN_LENGTH ->
            normalized.dropLast(IES_SUFFIX_LENGTH) + "y"
        normalized.endsWith("ses", ignoreCase = true) && normalized.length > PLURAL_IES_SUFFIX_MIN_LENGTH ->
            normalized.dropLast(SES_TO_S_DROP_LENGTH)
        normalized.endsWith("s", ignoreCase = true) &&
            !normalized.endsWith("ss", ignoreCase = true) &&
            normalized.length > TRAILING_S_MIN_LENGTH -> normalized.dropLast(1)
        else -> normalized
    }
}

private fun String.pluralize(): String {
    val normalized = trim()
    return when {
        normalized.endsWith("ies", ignoreCase = true) -> normalized
        normalized.endsWith("s", ignoreCase = true) -> normalized
        normalized.endsWith("y", ignoreCase = true) && normalized.length > TRAILING_S_MIN_LENGTH ->
            normalized.dropLast(1) + "ies"
        normalized.endsWith("x", ignoreCase = true) ||
            normalized.endsWith("z", ignoreCase = true) ||
            normalized.endsWith("ch", ignoreCase = true) ||
            normalized.endsWith("sh", ignoreCase = true) -> normalized + "es"
        else -> normalized + "s"
    }
}

private fun String.transformSingularized(enabled: Boolean): String = if (enabled) singularize() else this
