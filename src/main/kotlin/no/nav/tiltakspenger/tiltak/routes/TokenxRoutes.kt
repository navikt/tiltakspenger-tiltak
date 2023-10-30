package no.nav.tiltakspenger.tiltak.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import mu.KotlinLogging
import no.nav.tiltakspenger.tiltak.services.RoutesService

fun Route.tokenxRoutes(
    routesService: RoutesService,
) {
    val securelog = KotlinLogging.logger("tjenestekall")

    get("/tokenx/tiltak") {
        val ident = requireNotNull(call.principal<JWTPrincipal>()?.getClaim("pid", String::class)) { "pid er null i token" }
        val response = routesService.hentTiltak(ident)
        securelog.info { response }
        call.respond(message = response, status = HttpStatusCode.OK)
    }
}
