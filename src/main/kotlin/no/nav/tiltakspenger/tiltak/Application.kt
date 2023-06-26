
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import mu.KotlinLogging
import no.nav.tiltakspenger.tiltak.Configuration

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
    routing {
        get("/isalive") {
            call.respondText("ALIVE")
        }

        get("/isready") {
            call.respondText("READY")
        }
    }
}
