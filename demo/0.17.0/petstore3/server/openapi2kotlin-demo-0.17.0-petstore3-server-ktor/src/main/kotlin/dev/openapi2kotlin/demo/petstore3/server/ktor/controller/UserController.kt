package dev.openapi2kotlin.demo.petstore3.server.ktor.controller

import dev.openapi2kotlin.demo.model.User
import dev.openapi2kotlin.demo.server.UserApi

class UserController : UserApi {
    override suspend fun createUser(body: User?): User = body ?: demoUser("jane")

    override suspend fun createCreateWithList(body: List<User>?): User = body?.firstOrNull() ?: demoUser("jane")

    override suspend fun deleteUser(username: String) = Unit

    override suspend fun retrieveUser(username: String): User = demoUser(username)

    override suspend fun retrieveLogin(
        username: String?,
        password: String?,
    ): String = "Logged in ${username ?: "anonymous"}"

    override suspend fun retrieveLogout() = Unit

    override suspend fun updateUser(
        username: String,
        body: User?,
    ) = Unit

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
}
