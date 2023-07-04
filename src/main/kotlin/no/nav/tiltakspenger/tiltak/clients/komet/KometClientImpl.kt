package no.nav.tiltakspenger.tiltak.clients.komet

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.header
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import mu.KotlinLogging
import no.nav.tiltakspenger.tiltak.Configuration
import no.nav.tiltakspenger.tiltak.defaultHttpClient
import no.nav.tiltakspenger.tiltak.defaultObjectMapper

val securelog = KotlinLogging.logger("tjenestekall")

data class KometReqBody(
    val personIdent: String,
)

class KometClientImpl(
    private val config: Configuration.KometClientConfig = Configuration.kometClientConfig(),
    private val objectMapper: ObjectMapper = defaultObjectMapper(),
    private val getToken: suspend () -> String,
    engine: HttpClientEngine? = null,
    private val httpClient: HttpClient = defaultHttpClient(
        objectMapper = objectMapper,
        engine = engine,
    ),
) : KometClient {
    companion object {
        const val navCallIdHeader = "Nav-Call-Id"
    }

    override suspend fun hentTiltakDeltagelser(fnr: String): KometResponse {
        val httpResponse =
            httpClient.preparePost("${config.baseUrl}/api/external/deltakelser") {
                header(navCallIdHeader, "tiltakspenger-tiltak") // TODO hva skal vi bruke her?
                bearerAuth(getToken())
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                setBody(
                    KometReqBody(
                        personIdent = fnr,
                    ),
                )
            }.execute()

        return when (httpResponse.status) {
            HttpStatusCode.OK -> httpResponse.call.response.body()
//            HttpStatusCode.NotFound -> KometResponse(
//                deltakelser = emptyList()
//            ) // TODO sjekk om vi trenger denne
            else -> throw RuntimeException("error (responseCode=${httpResponse.status.value}) fra Komet")
        }
    }
}
