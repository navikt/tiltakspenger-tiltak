package no.nav.tiltakspenger.tiltak.clients.komet

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import no.nav.tiltakspenger.libs.common.AccessToken
import no.nav.tiltakspenger.tiltak.defaultObjectMapper
import no.nav.tiltakspenger.tiltak.httpClientWithRetry

data class KometReqBody(
    val norskIdent: String,
)

/**
 * Kildekode API: https://github.com/navikt/amt-tiltak/blob/main/external-api/src/main/kotlin/no/nav/amt/tiltak/external/api/ExternalAPI.kt
 * Per nå (7. aug. 2025) støtter Komet bare oppslag på fnr, ikke på tiltaksdeltagelse-id, slik at vi må gjøre filtreringen selv.
 */
class KometClientImpl(
    private val baseUrl: String,
    private val objectMapper: ObjectMapper = defaultObjectMapper(),
    private val getToken: suspend () -> AccessToken,
    engine: HttpClientEngine? = null,
    private val httpClient: HttpClient = httpClientWithRetry(
        objectMapper = objectMapper,
        engine = engine,
    ),
) : KometClient {
    companion object {
        const val NAV_CALL_ID_HEADER = "Nav-Call-Id"
    }
    private val log = KotlinLogging.logger {}

    override suspend fun hentTiltakDeltagelser(fnr: String, correlationId: String?): List<KometResponseJson> {
        val httpResponse =
            httpClient.post("$baseUrl/external/deltakelser") {
                header(NAV_CALL_ID_HEADER, correlationId)
                bearerAuth(getToken().token)
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                setBody(
                    KometReqBody(
                        norskIdent = fnr,
                    ),
                )
            }

        return when (httpResponse.status) {
            HttpStatusCode.OK -> httpResponse.call.response.body()
            else -> {
                log.error { "Mottok feilkode ved henting av tiltak fra Komet: ${httpResponse.status.value}" }
                throw RuntimeException("error (responseCode=${httpResponse.status.value}) fra Komet")
            }
        }
    }
}
