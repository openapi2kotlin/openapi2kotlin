package dev.openapi2kotlin.demo.petstore3.client.http4k

import dev.openapi2kotlin.demo.client.PetApi
import dev.openapi2kotlin.demo.client.PetApiImpl
import dev.openapi2kotlin.demo.client.StoreApi
import dev.openapi2kotlin.demo.client.StoreApiImpl
import dev.openapi2kotlin.demo.client.UserApi
import dev.openapi2kotlin.demo.client.UserApiImpl
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class Petstore3ClientConfiguration(
    props: Petstore3Props = Petstore3Props.from(),
) {
    private val httpClient: HttpHandler = javaHttpClient(props.url)

    fun petApi(): PetApi = PetApiImpl(httpClient)

    fun storeApi(): StoreApi = StoreApiImpl(httpClient)

    fun userApi(): UserApi = UserApiImpl(httpClient)
}

private fun javaHttpClient(baseUrl: String): HttpHandler {
    val client = HttpClient.newHttpClient()
    return { request: Request ->
        val requestBody = request.bodyString()
        val requestBuilder = HttpRequest.newBuilder(URI.create(baseUrl + request.uri))

        request.headers.forEach { (name, value) ->
            requestBuilder.header(name, value)
        }
        requestBuilder.method(
            request.method.name,
            if (requestBody.isEmpty()) {
                HttpRequest.BodyPublishers.noBody()
            } else {
                HttpRequest.BodyPublishers.ofString(requestBody)
            },
        )

        val response = client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString())
        var http4kResponse = Response(requireNotNull(Status.fromCode(response.statusCode()))).body(response.body())
        response.headers().map().forEach { (name, values) ->
            values.forEach { value ->
                http4kResponse = http4kResponse.header(name, value)
            }
        }
        http4kResponse
    }
}
