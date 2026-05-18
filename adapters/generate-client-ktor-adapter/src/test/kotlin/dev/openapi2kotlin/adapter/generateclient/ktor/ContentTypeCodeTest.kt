package dev.openapi2kotlin.adapter.generateclient.ktor

import kotlin.test.Test
import kotlin.test.assertEquals

class ContentTypeCodeTest {
    @Test
    fun `maps arbitrary spec media type via ContentType parse`() {
        assertEquals(
            """io.ktor.http.ContentType.parse("application/merge-patch+json")""",
            "application/merge-patch+json".toContentTypeCode().toString(),
        )
    }
}
