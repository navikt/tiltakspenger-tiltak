package no.nav.tiltakspenger.tiltak

import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing
import mu.KotlinLogging
import no.nav.tiltakspenger.tiltak.Configuration.kjøreMiljø
import no.nav.tiltakspenger.tiltak.routes.healthRoutes
import no.nav.tiltakspenger.tiltak.routes.routes

fun main() {
    System.setProperty("logback.configurationFile", Configuration.logbackConfigurationFile())

    val log = KotlinLogging.logger {}
    val securelog = KotlinLogging.logger("tjenestekall")

    Thread.setDefaultUncaughtExceptionHandler { _, e ->
        log.error { "Uncaught exception logget i securelog" }
        securelog.error(e) { e.message }
    }

    log.info { "starting server" }
    securelog.info { "testing securelog" }

    embeddedServer(
        factory = Netty,
        port = 8080,
        module = Application::tiltak,
    ).start(true)
}

fun Application.tiltak() {
    setupRouting()
}

fun Application.setupRouting() {
    routing {
        healthRoutes()
        if (kjøreMiljø() != Profile.PROD) {
            routes()
        }
    }
}
