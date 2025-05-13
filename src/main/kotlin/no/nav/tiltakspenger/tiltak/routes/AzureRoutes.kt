package no.nav.tiltakspenger.tiltak.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import no.nav.tiltakspenger.libs.logging.Sikkerlogg
import no.nav.tiltakspenger.tiltak.services.RoutesService

fun Route.azureRoutes(
    routesService: RoutesService,
) {
    data class RequestBody(
        val ident: String,
    )

    get("/azure/tiltak") {
        val ident = requireNotNull(call.principal<JWTPrincipal>()?.getClaim("ident", String::class)) { "ident er null i token" }
        val correlationId = call.request.headers["Nav-Call-Id"]
        val response = routesService.hentTiltakForSaksbehandling(ident, correlationId)

        Sikkerlogg.info { response }
        call.respond(message = response, status = HttpStatusCode.OK)
    }

    post("/azure/tiltak") {
        val ident = call.receive<RequestBody>().ident
        val correlationId = call.request.headers["Nav-Call-Id"]

        val response = routesService.hentTiltakForSaksbehandling(ident, correlationId)
        Sikkerlogg.info { response }
        call.respond(message = response, status = HttpStatusCode.OK)
    }
}
