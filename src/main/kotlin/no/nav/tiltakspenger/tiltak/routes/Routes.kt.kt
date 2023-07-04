package no.nav.tiltakspenger.tiltak.routes

import io.ktor.server.application.call
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import no.nav.tiltakspenger.tiltak.services.RoutesService

fun Route.routes(
    routesService: RoutesService,
) {
    get("/test/") {
        val ident = call.request.queryParameters["ident"] ?: "09015607561"
        routesService.hentTiltak(ident)
        call.respondText("TEST")
    }
}
