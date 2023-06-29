package no.nav.tiltakspenger.tiltak.routes

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.routes() {
    get("/test") {
        call.respondText("TEST")
    }
}