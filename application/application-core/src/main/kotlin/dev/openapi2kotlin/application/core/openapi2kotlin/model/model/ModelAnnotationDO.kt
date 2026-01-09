package dev.openapi2kotlin.application.core.openapi2kotlin.model.model

/**
 * fqName is e.g. "com.fasterxml.jackson.annotation.JsonProperty".
 * argsCode are Kotlin code snippets e.g. "\"@type\"" or "value = [\"@type\"]".
 */
data class ModelAnnotationDO(
    val useSite: UseSiteDO = UseSiteDO.NONE,
    val fqName: String,
    val argsCode: List<String> = emptyList(),
    val metadata: Map<String, String> = emptyMap(),
) {
    enum class UseSiteDO {
        NONE,
        PARAM,
        GET,
        SET,
        FIELD,
    }
}
