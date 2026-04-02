package dev.openapi2kotlin.demo.petstore3.server.http4k

import dev.openapi2kotlin.demo.petstore3.server.http4k.controller.PetController
import dev.openapi2kotlin.demo.petstore3.server.http4k.controller.StoreController
import dev.openapi2kotlin.demo.petstore3.server.http4k.controller.UserController
import dev.openapi2kotlin.demo.server.petRoutes
import dev.openapi2kotlin.demo.server.storeRoutes
import dev.openapi2kotlin.demo.server.userRoutes
import org.http4k.core.HttpHandler
import org.http4k.routing.routes

fun petstore3RoutesConfiguration(): HttpHandler =
    routes(
        petRoutes(PetController()),
        storeRoutes(StoreController()),
        userRoutes(UserController()),
    )
