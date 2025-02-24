package no.nav.tiltakspenger.tiltak.auth

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.ProxyBuilder
import io.ktor.client.engine.http
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.http.Parameters
import io.ktor.http.isSuccess
import mu.KotlinLogging
import no.nav.tiltakspenger.tiltak.defaultObjectMapper
import no.nav.tiltakspenger.tiltak.httpClientWithRetry

private val LOG = KotlinLogging.logger {}

fun interface TokenProvider {
    suspend fun getToken(): String
}

class AzureTokenProvider(
    objectMapper: ObjectMapper = defaultObjectMapper(),
    engine: HttpClientEngine? = null,
    private val config: OauthConfig,
) : TokenProvider {
    private val azureHttpClient = httpClientWithRetry(objectMapper = objectMapper, engine = engine) {
        System.getenv("HTTP_PROXY")?.let {
            LOG.info("Setter opp proxy mot $it")
            this.proxy = ProxyBuilder.http(it)
        }
    }

    private val tokenCache = TokenCache()

    override suspend fun getToken(): String {
        try {
            val currentToken = tokenCache.token
            return if (currentToken != null && !tokenCache.isExpired()) {
                currentToken
            } else {
                clientCredentials()
            }
        } catch (e: Exception) {
            throw AzureAuthException(e)
        }
    }

    private suspend fun wellknown(): WellKnown {
        val response = azureHttpClient.get(config.wellknownUrl)
        if (response.status.isSuccess()) {
            return response.body()
        } else {
            throw RuntimeException("Kunne ikke hente wellknown")
        }
    }

    private suspend fun clientCredentials(): String {
        val response = azureHttpClient.submitForm(
            url = wellknown().tokenEndpoint,
            formParameters = Parameters.build {
                append("grant_type", "client_credentials")
                append("client_id", config.clientId)
                append("client_secret", config.clientSecret)
                append("scope", config.scope)
            },
        )
        if (response.status.isSuccess()) {
            return response.body<OAuth2AccessTokenResponse>().let {
                tokenCache.update(
                    it.accessToken,
                    it.expiresIn.toLong(),
                )
                return@let it.accessToken
            }
        } else {
            throw RuntimeException("Kunne ikke hente token")
        }
    }

    data class OauthConfig(
        val scope: String,
        val clientId: String,
        val clientSecret: String,
        val wellknownUrl: String,
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class WellKnown(
        @JsonProperty("token_endpoint")
        val tokenEndpoint: String,
    )

    class AzureAuthException(e: Exception) : RuntimeException(e)
}
