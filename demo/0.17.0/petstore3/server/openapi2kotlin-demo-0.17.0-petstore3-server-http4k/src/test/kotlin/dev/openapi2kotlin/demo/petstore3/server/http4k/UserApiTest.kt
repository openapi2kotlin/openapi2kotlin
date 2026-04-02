package dev.openapi2kotlin.demo.petstore3.server.http4k

import dev.openapi2kotlin.demo.model.User
import dev.openapi2kotlin.demo.petstore3.server.http4k.tools.json
import dev.openapi2kotlin.demo.petstore3.server.http4k.tools.user
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status
import kotlin.test.Test
import kotlin.test.assertEquals

class UserApiTest {
    @Test
    fun `createUser returns posted user`() {
        val requestBody = user("alice")
        val response =
            application()(
                Request(Method.POST, "/user")
                    .header("Content-Type", "application/json")
                    .body(json.encodeToString(requestBody)),
            )

        assertEquals(Status.CREATED, response.status)
        assertEquals(requestBody, json.decodeFromString<User>(response.bodyString()))
    }

    @Test
    fun `retrieveUser returns seeded user`() {
        val response = application()(Request(Method.GET, "/user/dave"))

        assertEquals(Status.OK, response.status)
        assertEquals(user("dave"), json.decodeFromString<User>(response.bodyString()))
    }

    @Test
    fun `retrieveLogin returns a demo message`() {
        val response = application()(Request(Method.GET, "/user/login?username=eve&password=secret"))

        assertEquals(Status.OK, response.status)
        assertEquals("Logged in eve", response.bodyString())
        assertEquals("petstore3", response.header("X-Demo"))
    }
}
