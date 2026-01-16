package no.nav.tiltakspenger.tiltak.clients.tiltakshistorikk

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import no.nav.tiltakspenger.libs.common.AccessToken
import no.nav.tiltakspenger.tiltak.clients.tiltakshistorikk.dto.NorskIdent
import no.nav.tiltakspenger.tiltak.clients.tiltakshistorikk.dto.TiltakshistorikkV1Dto
import no.nav.tiltakspenger.tiltak.clients.tiltakshistorikk.dto.TiltakshistorikkV1Request
import no.nav.tiltakspenger.tiltak.clients.tiltakshistorikk.dto.TiltakshistorikkV1Response
import no.nav.tiltakspenger.tiltak.defaultObjectMapper
import no.nav.tiltakspenger.tiltak.httpClientWithRetry

/**
 * https://github.com/navikt/mulighetsrommet/tree/main/mulighetsrommet-tiltakshistorikk
 * Tjeneste som tilbys av Team Valp som leverer tiltaksdeltakelser uavhengig av kildesystem (kan være Arena, Komet eller Team Tiltak)
 */
class TiltakshistorikkClient(
    private val baseUrl: String,
    private val objectMapper: ObjectMapper = defaultObjectMapper(),
    private val getToken: suspend () -> AccessToken,
    engine: HttpClientEngine? = null,
    private val httpClient: HttpClient = httpClientWithRetry(
        objectMapper = objectMapper,
        engine = engine,
    ),
) {
    private val log = KotlinLogging.logger {}

    suspend fun hentTiltaksdeltakelser(fnr: String): List<TiltakshistorikkV1Dto> {
        val httpResponse =
            httpClient.post("$baseUrl/api/v1/historikk") {
                bearerAuth(getToken().token)
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                setBody(
                    TiltakshistorikkV1Request(
                        identer = listOf(NorskIdent(fnr)),
                    ),
                )
            }

        return when (httpResponse.status) {
            HttpStatusCode.OK -> httpResponse.call.response.body<TiltakshistorikkV1Response>().historikk
            else -> {
                val feilmelding = httpResponse.bodyAsText()
                log.error { "Mottok feilkode ved henting av tiltak fra tiltakshistorikk: ${httpResponse.status.value}, melding: $feilmelding" }
                throw RuntimeException("Feilkode ${httpResponse.status.value} fra tiltakshistorikk")
            }
        }
    }
}
