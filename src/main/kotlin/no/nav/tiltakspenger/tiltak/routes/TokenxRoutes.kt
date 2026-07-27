package no.nav.tiltakspenger.tiltak.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import no.nav.tiltakspenger.libs.ktor.common.respond500InternalServerError
import no.nav.tiltakspenger.libs.logging.Sikkerlogg
import no.nav.tiltakspenger.libs.texas.TexasPrincipalExternalUser
import no.nav.tiltakspenger.tiltak.services.TiltakshistorikkService

fun Route.tokenxRoutes(
    tiltakshistorikkService: TiltakshistorikkService,
) {
    get("/tokenx/tiltakshistorikk") {
        val ident = call.principal<TexasPrincipalExternalUser>()?.fnr?.verdi ?: throw IllegalStateException("Mangler principal")
        tiltakshistorikkService.hentTiltakshistorikkForSoknad(ident).fold(
            // Feilen er allerede logget i servicen; statusen er den samme 500-en konsumentene fikk da klientene kastet.
            ifLeft = { call.respond500InternalServerError("Noe gikk galt på serversiden", "server_feil") },
            ifRight = { response ->
                Sikkerlogg.info { response }
                call.respond(message = response, status = HttpStatusCode.OK)
            },
        )
    }
}
