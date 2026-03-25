package dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.model.helpers

/**
 * Convert OpenAPI property name to Kotlin identifier.
 *
 * Examples:
 * - "@type" -> "atType"
 * - "Id" -> "id"
 * - "CategoryId" -> "categoryId"
 * - "EAN" -> "ean"
 */
internal fun String.toKotlinName(): String =
    when {
        startsWith("@") && length > 1 ->
            listOf("at") + substring(1).splitIdentifierWords()

        else ->
            splitIdentifierWords()
    }
        .toLowerCamelPropertyName()
        .ensureKotlinIdentifier(defaultName = "value")

private fun List<String>.toLowerCamelPropertyName(): String =
    firstOrNull()?.lowercase().orEmpty() +
        drop(1).joinToString("") { token ->
            if (token.length <= 2 && token.all(Char::isUpperCase)) {
                token
            } else {
                token.lowercase().replaceFirstChar(Char::uppercaseChar)
            }
        }
