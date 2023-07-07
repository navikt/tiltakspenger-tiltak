package no.nav.tiltakspenger.tiltak.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import mu.KotlinLogging
import no.nav.tiltakspenger.tiltak.services.RoutesService
import no.nav.tiltakspenger.tiltak.services.TiltakDeltakelseResponse

val securelog = KotlinLogging.logger("tjenestekall")

data class Response(
    val deltakelser: List<TiltakDeltakelseResponse>,
)

fun Route.routes(
    routesService: RoutesService,
) {
    get("/test/") {
        val ident = call.request.queryParameters["ident"] ?: "09015607561"
        val response = Response(
            deltakelser = routesService.hentTiltak(ident),
        )

        securelog.info { response }

        call.respond(message = response, status = HttpStatusCode.OK)
    }
}
