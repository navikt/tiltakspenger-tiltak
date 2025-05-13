package no.nav.tiltakspenger.tiltak

import com.auth0.jwk.JwkProviderBuilder
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.plugins.callid.CallId
import io.ktor.server.plugins.callid.callIdMdc
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.response.respond
import io.ktor.server.routing.routing
import no.nav.tiltakspenger.libs.logging.Sikkerlogg
import no.nav.tiltakspenger.tiltak.routes.azureRoutes
import no.nav.tiltakspenger.tiltak.routes.healthRoutes
import no.nav.tiltakspenger.tiltak.routes.tokenxRoutes
import no.nav.tiltakspenger.tiltak.services.RoutesService
import java.net.URI
import java.util.UUID
import java.util.concurrent.TimeUnit

private val LOG = KotlinLogging.logger {}

fun Application.tiltakApi(
    routesService: RoutesService,
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
    val tokenxValidationConfig = Configuration.tokenxValidationConfig()
    val azureValidationConfig = Configuration.azureValidationConfig()

    val azureTokenProvider = JwkProviderBuilder(URI(azureValidationConfig.jwksUri).toURL())
        .cached(10, 24, TimeUnit.HOURS)
        .rateLimited(10, 1, TimeUnit.MINUTES)
        .build()
    val tokenxTokenProvider = JwkProviderBuilder(URI(tokenxValidationConfig.jwksUri).toURL())
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
            challenge { _, _ ->
                LOG.info { "verifier feilet" }
                call.respond(HttpStatusCode.Unauthorized, "Ikke tilgang! Issuer: ${tokenxValidationConfig.issuer}")
            }
            validate { credential ->
                if (credential.audience.contains(tokenxValidationConfig.clientId) &&
                    credential.payload.getClaim("pid")
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
        filter { call ->
            !call.request.path().contains("/isalive") &&
                !call.request.path().contains("/isready") &&
                !call.request.path().contains("/metrics")
        }
        format { call ->
            val status = call.response.status()
            val httpMethod = call.request.httpMethod.value
            val req = call.request
            val userAgent = call.request.headers["User-Agent"]
            Sikkerlogg.info { "Status: $status, HTTP method: $httpMethod, User agent: $userAgent req: $req" }
            "Status: $status, HTTP method: $httpMethod, User agent: $userAgent"
        }
    }
}
