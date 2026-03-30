package dev.openapi2kotlin.demo.petstore3.client.ktor

import io.ktor.server.application.Application

fun Application.module() {
    petstore3ClientConfiguration()
}
