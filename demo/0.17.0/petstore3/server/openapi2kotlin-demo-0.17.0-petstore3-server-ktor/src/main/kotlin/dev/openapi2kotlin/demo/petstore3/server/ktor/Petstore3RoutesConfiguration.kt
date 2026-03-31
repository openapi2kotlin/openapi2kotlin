package dev.openapi2kotlin.demo.petstore3.server.ktor

import dev.openapi2kotlin.demo.petstore3.server.ktor.controller.PetController
import dev.openapi2kotlin.demo.petstore3.server.ktor.controller.StoreController
import dev.openapi2kotlin.demo.petstore3.server.ktor.controller.UserController
import dev.openapi2kotlin.demo.server.petRoutes
import dev.openapi2kotlin.demo.server.storeRoutes
import dev.openapi2kotlin.demo.server.userRoutes
import io.ktor.server.application.Application
import io.ktor.server.routing.routing

fun Application.petstore3RoutesConfiguration() {
    routing {
        petRoutes(PetController())
        storeRoutes(StoreController())
        userRoutes(UserController())
    }
}
