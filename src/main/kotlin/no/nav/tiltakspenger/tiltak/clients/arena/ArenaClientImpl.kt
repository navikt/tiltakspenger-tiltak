package no.nav.tiltakspenger.tiltak.clients.arena

import com.fasterxml.jackson.databind.ObjectMapper
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
import no.nav.tiltakspenger.tiltak.Configuration
import no.nav.tiltakspenger.tiltak.defaultHttpClient
import no.nav.tiltakspenger.tiltak.defaultObjectMapper

// {
//    "id": "1c51c943-ce2d-4029-8c1e-18b3c59d3e2e",
//    "gjennomforing": {
//    "id": "bc4a05a5-56ed-47ac-8176-b685b0731751",
//    "navn": "Testing Linn 1",
//    "type": "INDOPPFAG",
//    "arrangor": {
//    "virksomhetsnummer": "974548283",
//    "navn": "TINN KOMMUNE KOMMUNEDIREKTØRENS STAB"
// },
//    "valp": {
//    "id": "bc4a05a5-56ed-47ac-8176-b685b0731751",
//    "tiltakstype": {
//    "id": "71a51692-35c5-4951-84eb-a338b0a57210",
//    "navn": "Oppfølging",
//    "arenaKode": "INDOPPFAG"
// },
//    "navn": "Testing Linn 1",
//    "startDato": "2022-01-01",
//    "sluttDato": "2023-01-01",
//    "status": "AVSLUTTET",
//    "virksomhetsnummer": "974548283",
//    "oppstart": "LOPENDE"
// },
//    "arenatiltak": {
//    "tiltaksaktiviteter": [],
//    "feil": null
// }
// },
//    "startDato": null,
//    "sluttDato": null,
//    "status": "IKKE_AKTUELL",
//    "dagerPerUke": 2.0,
//    "prosentStilling": 100.0,
//    "registrertDato": "2022-02-17T14:53:31"
// },

data class RequestBody(
    val ident: String,
)

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

    override suspend fun hentTiltakArena(fnr: String): List<TiltaksaktivitetDTO> {
        return kallArena(fnr)?.let {
            if (it.feil != null) throw RuntimeException("Kall til Arena feil med ${it.feil}")
            if (it.tiltaksaktiviteter == null) emptyList<TiltaksaktivitetDTO>()
            it.tiltaksaktiviteter
        } ?: emptyList()
    }

    private suspend fun kallArena(fnr: String): ArenaTiltaksaktivitetResponsDTO? {
        val httpResponse =
            httpClient.post("${config.baseUrl}/tiltakAzure") {
                header(navCallIdHeader, "tiltakspenger-tiltak") // TODO hva skal vi bruke her?
                bearerAuth(getToken())
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
            else -> throw RuntimeException("error (responseCode=${httpResponse.status.value}) fra Tiltakspenger-Arena")
        }
    }
}
