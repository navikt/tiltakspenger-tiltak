package no.nav.tiltakspenger.tiltak.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.logging.Sikkerlogg
import no.nav.tiltakspenger.libs.texas.TexasPrincipalExternalUser
import no.nav.tiltakspenger.tiltak.services.TiltakshistorikkService

fun Route.tokenxRoutes(
    tiltakshistorikkService: TiltakshistorikkService,
) {
    get("/tokenx/tiltakshistorikk") {
        val ident = call.principal<TexasPrincipalExternalUser>()?.fnr?.verdi ?: throw IllegalStateException("Mangler principal")
        val correlationId = CorrelationId.generate()
        val response = tiltakshistorikkService.hentTiltakshistorikkForSoknad(ident, correlationId.value)

        Sikkerlogg.info { response }
        call.respond(message = response, status = HttpStatusCode.OK)
    }
}
