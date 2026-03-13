package dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.model.helpers

internal fun resolveSchemaPackageName(
    basePackage: String,
    originalSchemaName: String,
): String {
    val nestedSegments = originalSchemaName
        .split('.')
        .dropLast(1)
        .mapNotNull { it.toPackageSegmentOrNull() }

    if (nestedSegments.isEmpty()) return basePackage
    return (listOf(basePackage) + nestedSegments).joinToString(".")
}

private fun String.toPackageSegmentOrNull(): String? {
    val cleaned = lowercase()
        .replace(Regex("[^a-z0-9_]"), "")
        .trim('_')
        .ifBlank { return null }

    return if (cleaned.first().isDigit()) "_$cleaned" else cleaned
}

