package no.nav.tiltakspenger.tiltak

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.engine.cio.CIOEngineConfig
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.ContentType
import io.ktor.serialization.jackson3.JacksonConverter
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.logging.Sikkerlogg
import tools.jackson.databind.json.JsonMapper
import java.time.Duration

private val LOG = KotlinLogging.logger {}
private const val SIXTY_SECONDS = 60L

// engine skal brukes primært i test-øyemed, når man sender med MockEngine.
// Forøvrig kan man la den være null.
fun defaultHttpClient(
    jsonMapper: JsonMapper = objectMapper,
    engine: HttpClientEngine? = null,
    configBlock: HttpClientConfig<*>.() -> Unit = {},
    engineConfigBlock: CIOEngineConfig.() -> Unit = {},
) = engine?.let {
    HttpClient(engine) {
        apply(defaultSetup(jsonMapper))
        apply(configBlock)
    }
} ?: HttpClient(CIO) {
    apply(defaultSetup(jsonMapper))
    apply(configBlock)
    engine(engineConfigBlock)
}

fun httpClientWithRetry(
    jsonMapper: JsonMapper,
    engine: HttpClientEngine? = null,
    configBlock: HttpClientConfig<*>.() -> Unit = {},
    engineConfigBlock: CIOEngineConfig.() -> Unit = {},
) =
    defaultHttpClient(
        jsonMapper = jsonMapper,
        engine = engine,
        configBlock = configBlock,
        engineConfigBlock = engineConfigBlock,
    )
        .config {
            install(HttpRequestRetry) {
                maxRetries = 3
                retryIf { request, response ->
                    if (response.status.value.let { it in 500..599 }) {
                        LOG.warn { "Http-kall feilet med ${response.status.value}. Kjører retry" }
                        true
                    } else {
                        false
                    }
                }
                retryOnExceptionIf { request, throwable ->
                    LOG.warn { "Kastet exception ved http-kall: ${throwable.message}" }
                    true
                }
                constantDelay(100, 0, false)
            }
        }

private fun defaultSetup(jsonMapper: JsonMapper): HttpClientConfig<*>.() -> Unit = {
    install(ContentNegotiation) {
        register(ContentType.Application.Json, JacksonConverter(jsonMapper))
    }
    install(HttpTimeout) {
        connectTimeoutMillis = Duration.ofSeconds(SIXTY_SECONDS).toMillis()
        requestTimeoutMillis = Duration.ofSeconds(SIXTY_SECONDS).toMillis()
        socketTimeoutMillis = Duration.ofSeconds(SIXTY_SECONDS).toMillis()
    }

    this.install(Logging) {
        logger = object : Logger {
            override fun log(message: String) {
                LOG.info { "HttpClient detaljer logget til securelog" }
                Sikkerlogg.info { message }
            }
        }
        level = LogLevel.INFO
    }
    this.expectSuccess = false
}
