package no.nav.tiltakspenger.tiltak.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.logging.Sikkerlogg
import no.nav.tiltakspenger.libs.texas.TexasPrincipalExternalUser
import no.nav.tiltakspenger.tiltak.services.RoutesService
import no.nav.tiltakspenger.tiltak.services.TiltakshistorikkService

fun Route.tokenxRoutes(
    routesService: RoutesService,
    tiltakshistorikkService: TiltakshistorikkService,
) {
    get("/tokenx/tiltak") {
        val ident = call.principal<TexasPrincipalExternalUser>()?.fnr?.verdi ?: throw IllegalStateException("Mangler principal")
        // Genereres her foreløpig til den legges ved i kallet fra soknad-api
        val correlationId = CorrelationId.generate()
        val response = routesService.hentTiltakForSøknad(ident, correlationId.value)

        Sikkerlogg.info { response }
        call.respond(message = response, status = HttpStatusCode.OK)
    }

    get("/tokenx/tiltakshistorikk") {
        val ident = call.principal<TexasPrincipalExternalUser>()?.fnr?.verdi ?: throw IllegalStateException("Mangler principal")
        val correlationId = CorrelationId.generate()
        val response = tiltakshistorikkService.hentTiltakshistorikkForSoknad(ident, correlationId.value)

        Sikkerlogg.info { response }
        call.respond(message = response, status = HttpStatusCode.OK)
    }
}
