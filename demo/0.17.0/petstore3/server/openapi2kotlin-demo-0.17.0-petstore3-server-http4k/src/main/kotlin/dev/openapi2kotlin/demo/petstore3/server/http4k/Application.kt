package dev.openapi2kotlin.demo.petstore3.server.http4k

import org.http4k.core.HttpHandler

fun application(): HttpHandler = petstore3RoutesConfiguration()
