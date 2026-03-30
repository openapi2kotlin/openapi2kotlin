package dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.api.helpers

import kotlin.test.Test
import kotlin.test.assertEquals

class HandleServerSwaggerAnnotationsTest {
    @Test
    fun `renders default response sentinel as default`() {
        assertEquals("default", renderSwaggerResponseCode(-1))
    }

    @Test
    fun `renders regular response codes as strings`() {
        assertEquals("200", renderSwaggerResponseCode(200))
        assertEquals("404", renderSwaggerResponseCode(404))
    }
}
