package no.nav.tiltakspenger.tiltak.clients.arena

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import no.nav.tiltakspenger.tiltak.Configuration
import no.nav.tiltakspenger.tiltak.defaultHttpClient
import no.nav.tiltakspenger.tiltak.defaultObjectMapper

class ArenaClientImpl(
    private val config: Configuration.ClientConfig = Configuration.arenaClientConfig(),
    private val objectMapper: ObjectMapper = defaultObjectMapper(),
    private val getToken: suspend () -> String,
    engine: HttpClientEngine? = null,
    private val httpClient: HttpClient = defaultHttpClient(
        objectMapper = objectMapper,
        engine = engine,
    ),
) : ArenaClient {
    companion object {
        const val navCallIdHeader = "Nav-Call-Id"
    }

    override suspend fun hentTiltakArena(fnr: String): ArenaDTO? {
        val httpResponse =
            httpClient.get("${config.baseUrl}/tiltak") {
                header(navCallIdHeader, "tiltakspenger-tiltak") // TODO hva skal vi bruke her?
                bearerAuth(getToken())
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
            }

        return when (httpResponse.status) {
            HttpStatusCode.OK -> httpResponse.call.response.body()
            HttpStatusCode.NotFound -> null
            else -> throw RuntimeException("error (responseCode=${httpResponse.status.value}) fra Tiltakspenger-Arena")
        }
    }
}
