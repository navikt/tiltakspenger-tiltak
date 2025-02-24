package no.nav.tiltakspenger.tiltak.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import no.nav.tiltakspenger.tiltak.isReady

/** Disse skal være helt åpne. */
fun Route.healthRoutes() {
    get("/isalive") {
        call.respondText("ALIVE")
    }

    get("/isready") {
        if (call.application.isReady()) {
            call.respondText("READY")
        } else {
            call.respondText("NOT READY", status = HttpStatusCode.ServiceUnavailable)
        }
    }
}
