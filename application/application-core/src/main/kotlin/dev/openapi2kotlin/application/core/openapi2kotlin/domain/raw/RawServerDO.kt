package dev.openapi2kotlin.application.core.openapi2kotlin.domain.raw

data class RawServerDO(
    val url: String?,
    val vars: List<Var>?,
) {
    data class Var(
        val name: String,
        val defaultValue: String,
    )
}
