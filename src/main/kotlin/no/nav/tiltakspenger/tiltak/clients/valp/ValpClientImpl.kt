package no.nav.tiltakspenger.tiltak.clients.valp

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
import mu.KotlinLogging
import no.nav.tiltakspenger.tiltak.Configuration
import no.nav.tiltakspenger.tiltak.defaultObjectMapper
import no.nav.tiltakspenger.tiltak.httpClientWithRetry

val securelog = KotlinLogging.logger("tjenestekall")

class ValpClientImpl(
    private val config: Configuration.ClientConfig = Configuration.valpClientConfig(),
    private val objectMapper: ObjectMapper = defaultObjectMapper(),
    private val getToken: suspend () -> String,
    engine: HttpClientEngine? = null,
    private val httpClient: HttpClient = httpClientWithRetry(
        objectMapper = objectMapper,
        engine = engine,
    ),
) : ValpClient {
    companion object {
        const val navCallIdHeader = "Nav-Call-Id"
    }

    override suspend fun hentTiltakGjennomføring(tiltakId: String): ValpDTO? {
        val httpResponse =
            httpClient.get("${config.baseUrl}/api/v1/tiltaksgjennomforinger/$tiltakId") {
                header(navCallIdHeader, "tiltakspenger-tiltak") // TODO hva skal vi bruke her?
                bearerAuth(getToken())
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
            }

        return when (httpResponse.status) {
            HttpStatusCode.OK -> httpResponse.call.response.body()
            HttpStatusCode.NotFound -> null
            else -> throw RuntimeException("error (responseCode=${httpResponse.status.value}) fra Valp")
        }
    }
}
