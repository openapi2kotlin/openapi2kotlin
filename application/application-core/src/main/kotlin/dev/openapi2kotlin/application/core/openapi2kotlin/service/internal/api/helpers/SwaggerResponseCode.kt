package dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.api.helpers

private const val DEFAULT_RESPONSE_STATUS_CODE = -1
private const val DEFAULT_RESPONSE_CODE = "default"

internal fun renderSwaggerResponseCode(statusCode: Int): String =
    if (statusCode == DEFAULT_RESPONSE_STATUS_CODE) {
        DEFAULT_RESPONSE_CODE
    } else {
        statusCode.toString()
    }
