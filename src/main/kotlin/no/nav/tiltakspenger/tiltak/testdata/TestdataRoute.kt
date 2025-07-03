package no.nav.tiltakspenger.tiltak.testdata

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post

fun Route.testdataRoutes(
    kometTestdataClient: KometTestdataClient,
) {
    post("/testdata/tiltaksdeltakelse/opprett") {
        val request = call.receive<OpprettTestDeltakelseRequest>()
        try {
            val response = kometTestdataClient.opprettTiltaksdeltakelse(request)
            call.respond(message = response, status = HttpStatusCode.OK)
        } catch (e: Exception) {
            call.respond(
                message = TestDeltakelseFeilmelding(
                    e.message ?: "Noe gikk galt ved oppretting av tiltaksdeltakelse",
                ),
                status = HttpStatusCode.InternalServerError,
            )
        }
    }
}

data class TestDeltakelseFeilmelding(
    val feilmelding: String,
)
