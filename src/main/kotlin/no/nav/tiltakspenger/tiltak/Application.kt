package no.nav.tiltakspenger.tiltak

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.util.AttributeKey

fun main() {
    System.setProperty("logback.configurationFile", Configuration.logbackConfigurationFile())

    val log = KotlinLogging.logger {}
    log.info { "starting server" }
    Thread.setDefaultUncaughtExceptionHandler { _, e ->
        log.error(e) { e }
    }
    val appBuilder = ApplicationBuilder()

    val server = embeddedServer(
        factory = Netty,
        port = 8080,
        module = {
            tiltakApi(appBuilder.routesService)
        },
    )
    server.application.attributes.put(isReadyKey, true)

    Runtime.getRuntime().addShutdownHook(
        Thread {
            server.application.attributes.put(isReadyKey, false)
            server.stop(gracePeriodMillis = 5_000, timeoutMillis = 5_000)
        },
    )

    server.start(wait = true)
}

val isReadyKey = AttributeKey<Boolean>("isReady")

fun Application.isReady() = attributes.getOrNull(isReadyKey) == true
