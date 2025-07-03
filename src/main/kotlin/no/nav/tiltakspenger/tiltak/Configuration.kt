package no.nav.tiltakspenger.tiltak

import com.natpryce.konfig.ConfigurationMap
import com.natpryce.konfig.ConfigurationProperties.Companion.systemProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.Key
import com.natpryce.konfig.overriding
import com.natpryce.konfig.stringType
import no.nav.tiltakspenger.tiltak.auth.AzureTokenProvider

enum class Profile {
    LOCAL,
    DEV,
    PROD,
}

object Configuration {

    private val otherDefaultProperties = mapOf(
        "application.httpPort" to 8080.toString(),
        "AZURE_APP_CLIENT_ID" to System.getenv("AZURE_APP_CLIENT_ID"),
        "AZURE_APP_CLIENT_SECRET" to System.getenv("AZURE_APP_CLIENT_SECRET"),
        "AZURE_APP_WELL_KNOWN_URL" to System.getenv("AZURE_APP_WELL_KNOWN_URL"),
        "TOKEN_X_CLIENT_ID" to System.getenv("TOKEN_X_CLIENT_ID"),
        "TOKEN_X_WELL_KNOWN_URL" to System.getenv("TOKEN_X_WELL_KNOWN_URL"),
        "TOKEN_X_ISSUER" to System.getenv("TOKEN_X_ISSUER"),
        "TOKEN_X_JWKS_URI" to System.getenv("TOKEN_X_JWKS_URI"),
        "logback.configurationFile" to "logback.xml",
    )

    private val defaultProperties = ConfigurationMap(otherDefaultProperties)

    private val localProperties = ConfigurationMap(
        mapOf(
            "application.profile" to Profile.LOCAL.toString(),
            "logback.configurationFile" to "logback.local.xml",
            "KOMET_URL" to "http://localhost",
            "KOMET_SCOPE" to "api://localhost/.default",
            "ARENA_URL" to "http://localhost",
            "ARENA_SCOPE" to "api://localhost/.default",
            "AZURE_APP_CLIENT_ID" to "xxx",
            "AZURE_APP_CLIENT_SECRET" to "YYY",
            "AZURE_APP_WELL_KNOWN_URL" to "ZZZ",
            "TOKEN_X_CLIENT_ID" to "xxx",
            "TOKEN_X_WELL_KNOWN_URL" to "http://host.docker.internal:6969/tokendings/.well-known/openid-configuration",
            "TOKEN_X_ISSUER" to "http://host.docker.internal:6969/default",
            "TOKEN_X_JWKS_URI" to "http://host.docker.internal:6969/default",
            "AZURE_OPENID_CONFIG_ISSUER" to "azure",
            "AZURE_OPENID_CONFIG_JWKS_URI" to "http://host.docker.internal:6969/azure/jwks",
            "KOMET_TESTDATA_URL" to "http://localhost",
            "KOMET_TESTDATA_SCOPE" to "api://localhost/.default",
        ),
    )
    private val devProperties = ConfigurationMap(
        mapOf(
            "application.profile" to Profile.DEV.toString(),
            "logback.configurationFile" to "logback.xml",
            "KOMET_URL" to "http://amt-deltaker.amt",
            "KOMET_SCOPE" to "api://dev-gcp.amt.amt-deltaker/.default",
            "ARENA_URL" to "https://tiltakspenger-arena.dev-fss-pub.nais.io",
            "ARENA_SCOPE" to "api://dev-fss.tpts.tiltakspenger-arena/.default",
            "KOMET_TESTDATA_URL" to "http://amt-deltaker-bff.amt",
            "KOMET_TESTDATA_SCOPE" to "api://dev-gcp.amt.amt-deltaker-bff/.default",
        ),
    )
    private val prodProperties = ConfigurationMap(
        mapOf(
            "application.profile" to Profile.PROD.toString(),
            "logback.configurationFile" to "logback.xml",
            "KOMET_URL" to "http://amt-deltaker.amt",
            "KOMET_SCOPE" to "api://prod-gcp.amt.amt-deltaker/.default",
            "ARENA_URL" to "https://tiltakspenger-arena.prod-fss-pub.nais.io",
            "ARENA_SCOPE" to "api://prod-fss.tpts.tiltakspenger-arena/.default",
            // testdata-apiet er ikke tilgjengelig i prod
            "KOMET_TESTDATA_URL" to "http://localhost",
            "KOMET_TESTDATA_SCOPE" to "api://localhost/.default",
        ),
    )

    private fun config() = when (System.getenv("NAIS_CLUSTER_NAME") ?: System.getProperty("NAIS_CLUSTER_NAME")) {
        "dev-gcp" ->
            systemProperties() overriding EnvironmentVariables overriding devProperties overriding defaultProperties

        "prod-gcp" ->
            systemProperties() overriding EnvironmentVariables overriding prodProperties overriding defaultProperties

        else -> {
            systemProperties() overriding EnvironmentVariables overriding localProperties overriding defaultProperties
        }
    }

    fun applicationProfile() = when (System.getenv("NAIS_CLUSTER_NAME") ?: System.getProperty("NAIS_CLUSTER_NAME")) {
        "dev-gcp" -> Profile.DEV
        "prod-gcp" -> Profile.PROD
        else -> Profile.LOCAL
    }

    fun isDev() = applicationProfile() == Profile.DEV

    fun isProd() = applicationProfile() == Profile.PROD

    fun logbackConfigurationFile() = config()[Key("logback.configurationFile", stringType)]

    fun kometClientConfig(baseUrl: String = config()[Key("KOMET_URL", stringType)]) =
        ClientConfig(baseUrl = baseUrl)

    fun arenaClientConfig(baseUrl: String = config()[Key("ARENA_URL", stringType)]) =
        ClientConfig(baseUrl = baseUrl)

    fun kometTestdataClientConfig(baseUrl: String = config()[Key("KOMET_TESTDATA_URL", stringType)]) =
        ClientConfig(baseUrl = baseUrl)

    fun tokenxValidationConfig(
        clientId: String = config()[Key("TOKEN_X_CLIENT_ID", stringType)],
        wellKnownUrl: String = config()[Key("TOKEN_X_WELL_KNOWN_URL", stringType)],
        issuer: String = config()[Key("TOKEN_X_ISSUER", stringType)],
        jwksUri: String = config()[Key("TOKEN_X_JWKS_URI", stringType)],
    ) = TokenValidationConfig(
        clientId = clientId,
        wellKnownUrl = wellKnownUrl,
        issuer = issuer,
        jwksUri = jwksUri,
    )

    fun azureValidationConfig(
        clientId: String = config()[Key("AZURE_APP_CLIENT_ID", stringType)],
        wellKnownUrl: String = config()[Key("AZURE_APP_WELL_KNOWN_URL", stringType)],
        issuer: String = config()[Key("AZURE_OPENID_CONFIG_ISSUER", stringType)],
        jwksUri: String = config()[Key("AZURE_OPENID_CONFIG_JWKS_URI", stringType)],
    ) = TokenValidationConfig(
        clientId = clientId,
        wellKnownUrl = wellKnownUrl,
        issuer = issuer,
        jwksUri = jwksUri,
    )

    data class TokenValidationConfig(
        val clientId: String,
        val wellKnownUrl: String,
        val issuer: String,
        val jwksUri: String,
    )

    data class ClientConfig(
        val baseUrl: String,
    )

    fun oauthConfigKomet(
        scope: String = config()[Key("KOMET_SCOPE", stringType)],
        clientId: String = config()[Key("AZURE_APP_CLIENT_ID", stringType)],
        clientSecret: String = config()[Key("AZURE_APP_CLIENT_SECRET", stringType)],
        wellknownUrl: String = config()[Key("AZURE_APP_WELL_KNOWN_URL", stringType)],
    ) = AzureTokenProvider.OauthConfig(
        scope = scope,
        clientId = clientId,
        clientSecret = clientSecret,
        wellknownUrl = wellknownUrl,
    )

    fun oauthConfigArena(
        scope: String = config()[Key("ARENA_SCOPE", stringType)],
        clientId: String = config()[Key("AZURE_APP_CLIENT_ID", stringType)],
        clientSecret: String = config()[Key("AZURE_APP_CLIENT_SECRET", stringType)],
        wellknownUrl: String = config()[Key("AZURE_APP_WELL_KNOWN_URL", stringType)],
    ) = AzureTokenProvider.OauthConfig(
        scope = scope,
        clientId = clientId,
        clientSecret = clientSecret,
        wellknownUrl = wellknownUrl,
    )

    fun oauthConfigKometTestdata(
        scope: String = config()[Key("KOMET_TESTDATA_SCOPE", stringType)],
        clientId: String = config()[Key("AZURE_APP_CLIENT_ID", stringType)],
        clientSecret: String = config()[Key("AZURE_APP_CLIENT_SECRET", stringType)],
        wellknownUrl: String = config()[Key("AZURE_APP_WELL_KNOWN_URL", stringType)],
    ) = AzureTokenProvider.OauthConfig(
        scope = scope,
        clientId = clientId,
        clientSecret = clientSecret,
        wellknownUrl = wellknownUrl,
    )
}
