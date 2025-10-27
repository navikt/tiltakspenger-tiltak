package no.nav.tiltakspenger.tiltak.clients.arena

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
import no.nav.tiltakspenger.libs.arena.tiltak.ArenaTiltaksaktivitetResponsDTO
import no.nav.tiltakspenger.libs.arena.tiltak.ArenaTiltaksaktivitetResponsDTO.TiltaksaktivitetDTO
import no.nav.tiltakspenger.libs.common.AccessToken
import no.nav.tiltakspenger.tiltak.defaultObjectMapper
import no.nav.tiltakspenger.tiltak.httpClientWithRetry

data class RequestBody(
    val ident: String,
)

class ArenaClient(
    private val baseUrl: String,
    private val objectMapper: ObjectMapper = defaultObjectMapper(),
    private val getToken: suspend () -> AccessToken,
    engine: HttpClientEngine? = null,
    private val httpClient: HttpClient = httpClientWithRetry(
        objectMapper = objectMapper,
        engine = engine,
    ),
) {
    companion object {
        const val NAV_CALL_ID_HEADER = "Nav-Call-Id"
    }
    private val log = KotlinLogging.logger {}

    suspend fun hentTiltakArena(fnr: String, correlationId: String?): List<TiltaksaktivitetDTO> {
        return kallArena(fnr, correlationId)?.let {
            if (it.feil != null) throw RuntimeException("Kall til Arena feil med ${it.feil}")
            if (it.tiltaksaktiviteter == null) emptyList<TiltaksaktivitetDTO>()
            it.tiltaksaktiviteter
        } ?: emptyList()
    }

    private suspend fun kallArena(fnr: String, correlationId: String?): ArenaTiltaksaktivitetResponsDTO? {
        val httpResponse =
            httpClient.post("$baseUrl/tiltakAzure") {
                header(NAV_CALL_ID_HEADER, correlationId)
                bearerAuth(getToken().token)
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                setBody(
                    RequestBody(
                        ident = fnr,
                    ),
                )
            }

        return when (httpResponse.status) {
            HttpStatusCode.OK -> httpResponse.call.response.body()
            HttpStatusCode.NotFound -> null
            else -> {
                log.error { "Mottok feilkode ved henting av tiltak fra Tiltakspenger-Arena: ${httpResponse.status.value}" }
                throw RuntimeException("error (responseCode=${httpResponse.status.value}) fra Tiltakspenger-Arena")
            }
        }
    }
}
