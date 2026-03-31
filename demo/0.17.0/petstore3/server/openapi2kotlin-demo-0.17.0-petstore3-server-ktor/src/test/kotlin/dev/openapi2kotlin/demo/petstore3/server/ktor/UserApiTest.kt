package dev.openapi2kotlin.demo.petstore3.server.ktor

import dev.openapi2kotlin.demo.model.User
import dev.openapi2kotlin.demo.petstore3.server.ktor.tools.AbstractApiTest
import dev.openapi2kotlin.demo.petstore3.server.ktor.tools.user
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlin.test.Test
import kotlin.test.assertEquals

class UserApiTest : AbstractApiTest() {
    @Test
    fun `createUser returns posted user`() =
        withApiTest {
            val requestBody = user("alice")
            val response =
                client.post("/user") {
                    contentType(ContentType.Application.Json)
                    setBody(requestBody)
                }

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(requestBody, response.body<User>())
        }

    @Test
    fun `retrieveUser returns seeded user`() =
        withApiTest {
            val response = client.get("/user/dave")

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(user("dave"), response.body<User>())
        }

    @Test
    fun `retrieveLogin returns a demo message`() =
        withApiTest {
            val response = client.get("/user/login?username=eve&password=secret")

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals("Logged in eve", response.body<String>())
        }
}
