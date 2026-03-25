package dev.openapi2kotlin.adapter.generatemodel.internal.helpers

import dev.openapi2kotlin.application.core.openapi2kotlin.model.model.ModelAnnotationDO

internal fun String.splitPackageAndSimple(): Pair<String, String> {
    val idx = lastIndexOf('.')
    require(idx > 0 && idx < length - 1) { "fqName must be fully-qualified, got: '$this'" }
    return substring(0, idx) to substring(idx + 1)
}

/**
 * Renders annotation metadata into generated Kotlin as KDoc lines.
 *
 * Kotlin/JVM annotations cannot carry arbitrary key/value pairs; metadata is therefore emitted as comments.
 *
 * Format:
 *   @meta <fqName> key1=value1; key2=value2
 */
internal fun ModelAnnotationDO.toMetadataKdocLineOrNull(): String? {
    if (metadata.isEmpty()) return null

    val payload =
        metadata.entries
            .sortedBy { it.key }
            .joinToString(separator = "; ") { (k, v) ->
                val vv = v.sanitizeForKdoc()
                "$k=$vv"
            }

    return "@meta $fqName $payload"
}

internal fun String.sanitizeForKdoc(): String =
    buildString {
        for (ch in this@sanitizeForKdoc) {
            when (ch) {
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(ch)
            }
        }
    }
