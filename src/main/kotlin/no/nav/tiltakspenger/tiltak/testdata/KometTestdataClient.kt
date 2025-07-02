package no.nav.tiltakspenger.tiltak.testdata

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import no.nav.tiltakspenger.tiltak.defaultHttpClient

class KometTestdataClient(
    private val httpClient: HttpClient = defaultHttpClient(),
    private val kometTestdataEndpoint: String,
    private val getToken: suspend () -> String,
) {

    private val log = KotlinLogging.logger {}

    suspend fun opprettTiltaksdeltakelse(opprettTestDeltakelseRequest: OpprettTestDeltakelseRequest) {
        val httpResponse = httpClient.post("$kometTestdataEndpoint/testdata/opprett") {
            bearerAuth(getToken())
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            setBody(opprettTestDeltakelseRequest)
        }

        if (!httpResponse.status.isSuccess()) {
            val responsebody = httpResponse.bodyAsText()
            val feilkode = httpResponse.status.value
            log.error { "Mottok feilkode ved oppretting av test-tiltaksdeltakelser fra Komet: $feilkode, $responsebody" }
            throw RuntimeException("Kunne ikke opprette deltakelse: $responsebody, feilkode $feilkode")
        }
        return httpResponse.body()
    }
}
