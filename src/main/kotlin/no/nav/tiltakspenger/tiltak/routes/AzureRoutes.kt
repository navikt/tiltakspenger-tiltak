package no.nav.tiltakspenger.tiltak.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import mu.KotlinLogging
import no.nav.tiltakspenger.tiltak.services.RoutesService
import no.nav.tiltakspenger.tiltak.services.TiltakDeltakelseResponse

fun Route.azureRoutes(
    routesService: RoutesService,
) {
    val securelog = KotlinLogging.logger("tjenestekall")
    data class Response(
        val deltakelser: List<TiltakDeltakelseResponse>,
    )

    data class RequestBody(
        val ident: String,
    )

    get("/azure/tiltak") {
        val ident = requireNotNull(call.principal<JWTPrincipal>()?.getClaim("ident", String::class)) { "ident er null i token" }

        val response = Response(
            deltakelser = routesService.hentTiltak(ident),
        )

        securelog.info { response }
        call.respond(message = response, status = HttpStatusCode.OK)
    }

    post("/azure/tiltak") {
        val ident = call.receive<RequestBody>().ident

        val response = Response(
            deltakelser = routesService.hentTiltak(ident),
        )

        securelog.info { response }
        call.respond(message = response, status = HttpStatusCode.OK)
    }
}
