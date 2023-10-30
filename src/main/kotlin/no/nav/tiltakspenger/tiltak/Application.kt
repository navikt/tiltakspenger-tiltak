package no.nav.tiltakspenger.tiltak

import com.auth0.jwk.JwkProviderBuilder
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.callid.CallId
import io.ktor.server.plugins.callid.callIdMdc
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import mu.KotlinLogging
import no.nav.tiltakspenger.tiltak.Configuration.azureValidationConfig
import no.nav.tiltakspenger.tiltak.Configuration.oauthConfigArena
import no.nav.tiltakspenger.tiltak.Configuration.oauthConfigKomet
import no.nav.tiltakspenger.tiltak.Configuration.oauthConfigTiltak
import no.nav.tiltakspenger.tiltak.Configuration.oauthConfigValp
import no.nav.tiltakspenger.tiltak.Configuration.tokenxValidationConfig
import no.nav.tiltakspenger.tiltak.auth.AzureTokenProvider
import no.nav.tiltakspenger.tiltak.clients.arena.ArenaClient
import no.nav.tiltakspenger.tiltak.clients.arena.ArenaClientImpl
import no.nav.tiltakspenger.tiltak.clients.komet.KometClientImpl
import no.nav.tiltakspenger.tiltak.clients.tiltak.TiltakClient
import no.nav.tiltakspenger.tiltak.clients.tiltak.TiltakClientImpl
import no.nav.tiltakspenger.tiltak.clients.valp.ValpClient
import no.nav.tiltakspenger.tiltak.clients.valp.ValpClientImpl
import no.nav.tiltakspenger.tiltak.routes.azureRoutes
import no.nav.tiltakspenger.tiltak.routes.healthRoutes
import no.nav.tiltakspenger.tiltak.routes.tokenxRoutes
import no.nav.tiltakspenger.tiltak.services.RouteServiceImpl
import no.nav.tiltakspenger.tiltak.services.RoutesService
import java.util.*
import java.util.concurrent.TimeUnit

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

// TODO Vi lager nå kun system token. Vi må sannsynligvis legge inn mulighet til å kalle med On Behalf Of Token
fun Application.tiltak(
    tokenProviderKomet: AzureTokenProvider = AzureTokenProvider(config = oauthConfigKomet()),
    tokenProviderValp: AzureTokenProvider = AzureTokenProvider(config = oauthConfigValp()),
    tokenProviderTiltak: AzureTokenProvider = AzureTokenProvider(config = oauthConfigTiltak()),
    tokenProviderArena: AzureTokenProvider = AzureTokenProvider(config = oauthConfigArena()),
    kometClient: KometClientImpl = KometClientImpl(
        getToken = tokenProviderKomet::getToken,
    ),
    valpClient: ValpClient = ValpClientImpl(
        getToken = tokenProviderValp::getToken,
    ),
    arenaClient: ArenaClient = ArenaClientImpl(
        getToken = tokenProviderArena::getToken,
    ),
    tiltakClient: TiltakClient = TiltakClientImpl(
        getToken = tokenProviderTiltak::getToken,
    ),
    routesService: RoutesService = RouteServiceImpl(
        kometClient = kometClient,
        valpClient = valpClient,
        tiltakClient = tiltakClient,
        arenaClient = arenaClient,
    ),
) {
    installCallLogging()
    setupRouting(routesService)
}

fun Application.setupRouting(
    routesService: RoutesService,
) {
    jacksonSerialization()
    installAuthentication()
    routing {
        healthRoutes()
        authenticate("tokenx") {
            tokenxRoutes(routesService)
        }
        authenticate("azure") {
            azureRoutes(routesService)
        }
    }
}

fun Application.installAuthentication() {
    val tokenxValidationConfig = tokenxValidationConfig()
    val azureValidationConfig = azureValidationConfig()

    val azureTokenProvider = JwkProviderBuilder(azureValidationConfig.jwksUri)
        .cached(10, 24, TimeUnit.HOURS)
        .rateLimited(10, 1, TimeUnit.MINUTES)
        .build()
    val tokenxTokenProvider = JwkProviderBuilder(tokenxValidationConfig.jwksUri)
        .cached(10, 24, TimeUnit.HOURS)
        .rateLimited(10, 1, TimeUnit.MINUTES)
        .build()

    install(Authentication) {
        jwt("azure") {
            verifier(azureTokenProvider, azureValidationConfig.issuer)
            validate { credential ->
                if (credential.audience.contains(azureValidationConfig.clientId)) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
        }
        jwt("tokenx") {
            verifier(tokenxTokenProvider, tokenxValidationConfig.issuer)
            validate { credential ->
                if (credential.audience.contains(tokenxValidationConfig.clientId) && credential.payload.getClaim("pid")
                        .asString() != ""
                ) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
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

internal fun Application.installCallLogging() {
    install(CallId) {
        generate { UUID.randomUUID().toString() }
    }
    install(CallLogging) {
        callIdMdc("call-id")
        disableDefaultColors()
    }
}
