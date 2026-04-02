package dev.openapi2kotlin.demo.petstore3.server.http4k.controller

import dev.openapi2kotlin.demo.model.User
import dev.openapi2kotlin.demo.server.UserApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.http4k.core.Response
import org.http4k.core.Status

class UserController : UserApi {
    override fun createUser(body: User?): User = body ?: demoUser("jane")

    override fun createUserWithHttpInfo(body: User?): Response =
        Response(Status.CREATED)
            .header("Content-Type", "application/json")
            .body(json.encodeToString(createUser(body)))

    override fun createCreateWithList(body: List<User>?): User = body?.firstOrNull() ?: demoUser("jane")

    override fun createCreateWithListWithHttpInfo(body: List<User>?): Response =
        Response(Status.OK)
            .header("Content-Type", "application/json")
            .body(json.encodeToString(createCreateWithList(body)))

    override fun deleteUser(username: String) = Unit

    override fun deleteUserWithHttpInfo(username: String): Response = Response(Status.NO_CONTENT)

    override fun retrieveUser(username: String): User = demoUser(username)

    override fun retrieveUserWithHttpInfo(username: String): Response =
        Response(Status.OK)
            .header("Content-Type", "application/json")
            .body(json.encodeToString(retrieveUser(username)))

    override fun retrieveLogin(
        username: String?,
        password: String?,
    ): String = "Logged in ${username ?: "anonymous"}"

    override fun retrieveLoginWithHttpInfo(
        username: String?,
        password: String?,
    ): Response =
        Response(Status.OK)
            .header("X-Demo", "petstore3")
            .body(retrieveLogin(username, password))

    override fun retrieveLogout() = Unit

    override fun retrieveLogoutWithHttpInfo(): Response = Response(Status.NO_CONTENT)

    override fun updateUser(
        username: String,
        body: User?,
    ) = Unit

    override fun updateUserWithHttpInfo(
        username: String,
        body: User?,
    ): Response = Response(Status.NO_CONTENT)

    private fun demoUser(username: String): User =
        User(
            id = 1,
            username = username,
            firstName = "Jane",
            lastName = "Doe",
            email = "$username@petstore3.demo",
            password = "secret",
            phone = "123456789",
            userStatus = 1,
        )

    private companion object {
        val json = Json { ignoreUnknownKeys = true }
    }
}
