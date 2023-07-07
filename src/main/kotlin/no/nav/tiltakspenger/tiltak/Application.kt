package no.nav.tiltakspenger.tiltak

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import mu.KotlinLogging
import no.nav.tiltakspenger.tiltak.Configuration.kjøreMiljø
import no.nav.tiltakspenger.tiltak.Configuration.oauthConfigKomet
import no.nav.tiltakspenger.tiltak.Configuration.oauthConfigValp
import no.nav.tiltakspenger.tiltak.auth.AzureTokenProvider
import no.nav.tiltakspenger.tiltak.clients.komet.KometClientImpl
import no.nav.tiltakspenger.tiltak.clients.valp.ValpClient
import no.nav.tiltakspenger.tiltak.clients.valp.ValpClientImpl
import no.nav.tiltakspenger.tiltak.routes.healthRoutes
import no.nav.tiltakspenger.tiltak.routes.routes
import no.nav.tiltakspenger.tiltak.services.RouteServiceImpl
import no.nav.tiltakspenger.tiltak.services.RoutesService

fun main() {
    System.setProperty("logback.configurationFile", Configuration.logbackConfigurationFile())

    val log = KotlinLogging.logger {}
    val securelog = KotlinLogging.logger("tjenestekall")

    Thread.setDefaultUncaughtExceptionHandler { _, e ->
        log.error { "Uncaught exception logget i securelog" }
        securelog.error(e) { e.message }
    }

    log.info { "starting server" }

    embeddedServer(
        factory = Netty,
        port = 8080,
        module = Application::tiltak,
    ).start(true)
}

fun Application.tiltak(
    tokenProviderKomet: AzureTokenProvider = AzureTokenProvider(config = oauthConfigKomet()),
    tokenProviderValp: AzureTokenProvider = AzureTokenProvider(config = oauthConfigValp()),
    kometClient: KometClientImpl = KometClientImpl(
        getToken = tokenProviderKomet::getToken,
    ),
    valpClient: ValpClient = ValpClientImpl(
        getToken = tokenProviderValp::getToken,
    ),
    routesService: RoutesService = RouteServiceImpl(
        kometClient = kometClient,
        valpClient = valpClient,
    ),
) {
    setupRouting(routesService)
}

fun Application.setupRouting(
    routesService: RoutesService,
) {
    jacksonSerialization()
    routing {
        healthRoutes()
        if (kjøreMiljø() != Profile.PROD) {
            routes(routesService)
        }
    }
}

fun Application.jacksonSerialization() {
    install(ContentNegotiation) {
        jackson {
            configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            registerModule(JavaTimeModule())
            registerModule(KotlinModule.Builder().build())
        }
    }
}
