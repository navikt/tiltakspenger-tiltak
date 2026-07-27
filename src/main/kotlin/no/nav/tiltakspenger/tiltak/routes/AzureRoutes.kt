package no.nav.tiltakspenger.tiltak.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.libs.ktor.common.respond500InternalServerError
import no.nav.tiltakspenger.libs.logging.Sikkerlogg
import no.nav.tiltakspenger.tiltak.services.TiltakshistorikkService

fun Route.azureRoutes(
    tiltakshistorikkService: TiltakshistorikkService,
) {
    data class RequestBody(
        val ident: String,
    )

    post("/azure/tiltakshistorikk") {
        val ident = call.receive<RequestBody>().ident
        tiltakshistorikkService.hentTiltakshistorikkForSaksbehandling(ident).fold(
            // Feilen er allerede logget i servicen; statusen er den samme 500-en konsumentene fikk da klientene kastet.
            ifLeft = { call.respond500InternalServerError("Noe gikk galt på serversiden", "server_feil") },
            ifRight = { response ->
                Sikkerlogg.info { response }
                call.respond(message = response, status = HttpStatusCode.OK)
            },
        )
    }
}
