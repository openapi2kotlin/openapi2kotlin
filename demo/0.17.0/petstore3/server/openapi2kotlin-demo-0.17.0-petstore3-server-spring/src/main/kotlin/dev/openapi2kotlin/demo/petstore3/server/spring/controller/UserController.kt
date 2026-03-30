package dev.openapi2kotlin.demo.petstore3.server.spring.controller

import dev.openapi2kotlin.demo.model.User
import dev.openapi2kotlin.demo.server.UserApi
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class UserController : UserApi {
    override fun createUser(body: User?): ResponseEntity<User> {
        return ResponseEntity.ok(body ?: demoUser("jane"))
    }

    override fun createCreateWithList(body: List<User>?): ResponseEntity<User> {
        return ResponseEntity.ok(body?.firstOrNull() ?: demoUser("jane"))
    }

    override fun deleteUser(username: String): ResponseEntity<Void> {
        return ResponseEntity.noContent().build()
    }

    override fun retrieveUser(username: String): ResponseEntity<User> {
        return ResponseEntity.ok(demoUser(username))
    }

    override fun retrieveLogin(
        username: String?,
        password: String?,
    ): ResponseEntity<String> {
        return ResponseEntity.ok("Logged in ${username ?: "anonymous"}")
    }

    override fun retrieveLogout(): ResponseEntity<Void> {
        return ResponseEntity.noContent().build()
    }

    override fun updateUser(
        username: String,
        body: User?,
    ): ResponseEntity<Void> {
        return ResponseEntity.noContent().build()
    }

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
