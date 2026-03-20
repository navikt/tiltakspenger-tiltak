package no.nav.tiltakspenger.journalposthendelser.journalpost.http.pdl

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import no.nav.tiltakspenger.libs.common.AccessToken
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.tiltak.infra.http.GraphQLResponse
import tools.jackson.module.kotlin.readValue

/**
 * https://pdl-docs.ansatt.nav.no/ekstern/index.html
 * Spørringen henter ikke historiske identer, kun gjeldende.
 */
class PdlClient(
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val getToken: suspend () -> AccessToken,
) {
    private val log = KotlinLogging.logger {}

    /**
     * @return null Dersom oppslaget feiler eller ikke returnerer på noe vis.
     */
    suspend fun hentNåværendeOgHistoriskeFødselsnummer(fnr: String): List<String>? {
        val httpResponse = httpClient.post("$baseUrl/graphql") {
            bearerAuth(getToken().token)
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            header("Tema", "IND")
            header("behandlingsnummer", "B470")
            setBody(
                HentIdenterRequest(
                    query = hentIdenterQuery,
                    variables = PdlVariables(
                        ident = fnr,
                    ),
                ),
            )
        }
        val responseBody = httpResponse.bodyAsText()
        if (!httpResponse.status.isSuccess()) {
            log.error { "Vi fikk ikke 200 OK ved kall til PDL: feilkode: ${httpResponse.status}, melding: $responseBody" }
            throw RuntimeException("Noe gikk galt ved kall til PDL")
        }
        val hentIdenterResponse = objectMapper.readValue<GraphQLResponse<HentIdenterResponse>?>(responseBody)

        if (hentIdenterResponse == null) {
            log.error { "Kall til PDL feilet. hentIdenterResponse var null" }
            return null
        }
        if (hentIdenterResponse.errors != null) {
            hentIdenterResponse.errors.forEach { log.error { "PDL returnerte feilmelding: $it" } }
            return null
        }
        if (hentIdenterResponse.data.hentIdenter == null || hentIdenterResponse.data.hentIdenter.identer.isEmpty()) {
            log.error { "Fant ingen identer i PDL" }
            return null
        }
        return hentIdenterResponse.data.hentIdenter.identer.map { it.ident }
    }
}

data class HentIdenterRequest(val query: String, val variables: PdlVariables)

data class PdlVariables(val ident: String)

data class HentIdenterResponse(
    val hentIdenter: Identliste?,
)

data class Identliste(
    val identer: List<IdentInformasjon>,
)

data class IdentInformasjon(
    val ident: String,
)

private val hentIdenterQuery = $$"""
    query($ident: ID!){
      hentIdenter(ident: $ident, grupper: FOLKEREGISTERIDENT, historikk: true) {
          identer {
            ident
          }
        }
    }
""".trimIndent()
