package no.nav.tiltakspenger.tiltak

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.authentication
import io.ktor.server.plugins.callid.CallId
import io.ktor.server.plugins.callid.callIdMdc
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.routing.routing
import no.nav.tiltakspenger.libs.texas.IdentityProvider
import no.nav.tiltakspenger.libs.texas.TexasAuthenticationProvider
import no.nav.tiltakspenger.libs.texas.client.TexasClient
import no.nav.tiltakspenger.tiltak.routes.azureRoutes
import no.nav.tiltakspenger.tiltak.routes.healthRoutes
import no.nav.tiltakspenger.tiltak.routes.swaggerRoute
import no.nav.tiltakspenger.tiltak.routes.tokenxRoutes
import no.nav.tiltakspenger.tiltak.services.RoutesService
import no.nav.tiltakspenger.tiltak.services.TiltakshistorikkService
import no.nav.tiltakspenger.tiltak.testdata.KometTestdataClient
import no.nav.tiltakspenger.tiltak.testdata.testdataRoutes
import java.util.UUID

fun Application.ktorSetup(
    routesService: RoutesService,
    kometTestdataClient: KometTestdataClient,
    texasClient: TexasClient,
    tiltakshistorikkService: TiltakshistorikkService,
) {
    installCallLogging()
    setupRouting(routesService, kometTestdataClient, texasClient, tiltakshistorikkService)
}

fun Application.setupRouting(
    routesService: RoutesService,
    kometTestdataClient: KometTestdataClient,
    texasClient: TexasClient,
    tiltakshistorikkService: TiltakshistorikkService,
) {
    jacksonSerialization()
    installAuthentication(texasClient)
    routing {
        healthRoutes()
        authenticate(IdentityProvider.TOKENX.value) {
            tokenxRoutes(routesService, tiltakshistorikkService)
        }
        authenticate(IdentityProvider.AZUREAD.value) {
            azureRoutes(routesService, tiltakshistorikkService)
            if (!Configuration.isProd()) {
                testdataRoutes(kometTestdataClient)
            }
        }
        if (Configuration.isDev()) {
            swaggerRoute()
        }
    }
}

fun Application.installAuthentication(texasClient: TexasClient) {
    authentication {
        register(
            TexasAuthenticationProvider(
                TexasAuthenticationProvider.Config(
                    name = IdentityProvider.TOKENX.value,
                    texasClient = texasClient,
                    identityProvider = IdentityProvider.TOKENX,
                    requireIdportenLevelHigh = false,
                ),
            ),
        )

        register(
            TexasAuthenticationProvider(
                TexasAuthenticationProvider.Config(
                    name = IdentityProvider.AZUREAD.value,
                    texasClient = texasClient,
                    identityProvider = IdentityProvider.AZUREAD,
                ),
            ),
        )
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
        filter { call ->
            !call.request.path().contains("/isalive") &&
                !call.request.path().contains("/isready") &&
                !call.request.path().contains("/metrics")
        }
        format { call ->
            val status = call.response.status()
            val httpMethod = call.request.httpMethod.value
            val userAgent = call.request.headers["User-Agent"]
            "Status: $status, HTTP method: $httpMethod, User agent: $userAgent"
        }
    }
}
