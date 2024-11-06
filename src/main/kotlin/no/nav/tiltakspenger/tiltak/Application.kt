package no.nav.tiltakspenger.tiltak

import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import mu.KotlinLogging

fun main() {
    System.setProperty("logback.configurationFile", Configuration.logbackConfigurationFile())

    val log = KotlinLogging.logger {}
    val securelog = KotlinLogging.logger("tjenestekall")
    log.info { "starting server" }
    Thread.setDefaultUncaughtExceptionHandler { _, e ->
        log.error { e }
        securelog.error(e) { e.message }
    }
    val appBuilder = ApplicationBuilder()

    embeddedServer(
        factory = Netty,
        port = 8080,
        module = {
            tiltakApi(appBuilder.routesService)
        },
    ).start(wait = true)
}
