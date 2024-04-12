package no.nav.tiltakspenger.tiltak

import com.natpryce.konfig.ConfigurationMap
import com.natpryce.konfig.ConfigurationProperties.Companion.systemProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.Key
import com.natpryce.konfig.overriding
import com.natpryce.konfig.stringType
import no.nav.tiltakspenger.tiltak.auth.AzureTokenProvider

enum class Profile {
    LOCAL, DEV, PROD
}

object Configuration {

    val rapidsAndRivers = mapOf(
        "RAPID_APP_NAME" to "tiltakspenger-tiltak",
        "KAFKA_BROKERS" to System.getenv("KAFKA_BROKERS"),
        "KAFKA_CREDSTORE_PASSWORD" to System.getenv("KAFKA_CREDSTORE_PASSWORD"),
        "KAFKA_TRUSTSTORE_PATH" to System.getenv("KAFKA_TRUSTSTORE_PATH"),
        "KAFKA_KEYSTORE_PATH" to System.getenv("KAFKA_KEYSTORE_PATH"),
        "KAFKA_RAPID_TOPIC" to "tpts.rapid.v1",
        "KAFKA_EXTRA_TOPIC" to "amt.deltaker-v1",
        "KAFKA_RESET_POLICY" to "latest",
        "KAFKA_CONSUMER_GROUP_ID" to "tiltakspenger-tiltak-v1",
    )

    private val otherDefaultProperties = mapOf(
        "application.httpPort" to 8080.toString(),
//        "SERVICEUSER_TPTS_USERNAME" to System.getenv("SERVICEUSER_TPTS_USERNAME"),
//        "SERVICEUSER_TPTS_PASSWORD" to System.getenv("SERVICEUSER_TPTS_PASSWORD"),
        "AZURE_APP_CLIENT_ID" to System.getenv("AZURE_APP_CLIENT_ID"),
        "AZURE_APP_CLIENT_SECRET" to System.getenv("AZURE_APP_CLIENT_SECRET"),
        "AZURE_APP_WELL_KNOWN_URL" to System.getenv("AZURE_APP_WELL_KNOWN_URL"),
        "TOKEN_X_CLIENT_ID" to System.getenv("TOKEN_X_CLIENT_ID"),
        "TOKEN_X_WELL_KNOWN_URL" to System.getenv("TOKEN_X_WELL_KNOWN_URL"),
        "TOKEN_X_ISSUER" to System.getenv("TOKEN_X_ISSUER"),
        "TOKEN_X_JWKS_URI" to System.getenv("TOKEN_X_JWKS_URI"),
        "logback.configurationFile" to "logback.xml",
    )

    private val defaultProperties = ConfigurationMap(rapidsAndRivers + otherDefaultProperties)

    private val localProperties = ConfigurationMap(
        mapOf(
            "application.profile" to Profile.LOCAL.toString(),
            "logback.configurationFile" to "logback.local.xml",
            "KOMET_URL" to "http://localhost", // TODO her kan vi legge inn stubbing?
            "KOMET_SCOPE" to "api://localhost/.default", // TODO her kan vi legge inn stubbing?
            "VALP_URL" to "http://localhost", // TODO her kan vi legge inn stubbing?
            "VALP_SCOPE" to "api://localhost/.default", // TODO her kan vi legge inn stubbing?
            "ARENA_URL" to "http://localhost", // TODO her kan vi legge inn stubbing?
            "ARENA_SCOPE" to "api://localhost/.default", // TODO her kan vi legge inn stubbing?
            "TILTAK_URL" to "http://localhost", // TODO her kan vi legge inn stubbing?
            "TILTAK_SCOPE" to "api://localhost/.default", // TODO her kan vi legge inn stubbing?
            "AZURE_APP_CLIENT_ID" to "xxx",
            "AZURE_APP_CLIENT_SECRET" to "YYY",
            "AZURE_APP_WELL_KNOWN_URL" to "ZZZ",
            "TOKEN_X_CLIENT_ID" to "xxx",
            "TOKEN_X_WELL_KNOWN_URL" to "http://localhost:8080/default/.well-known/openid-configuration",
            "TOKEN_X_ISSUER" to "http://localhost:8080/default",
            "TOKEN_X_JWKS_URI" to "http://localhost:8080/default",
            "AZURE_OPENID_CONFIG_ISSUER" to "azure",
            "AZURE_OPENID_CONFIG_JWKS_URI" to "AZURE_OPENID_CONFIG_JWKS_URI",
        ),
    )
    private val devProperties = ConfigurationMap(
        mapOf(
            "application.profile" to Profile.DEV.toString(),
            "logback.configurationFile" to "logback.xml",
            "KOMET_URL" to "http://amt-tiltak.amt",
            "KOMET_SCOPE" to "api://dev-gcp.amt.amt-tiltak/.default",
            "VALP_URL" to "http://mulighetsrommet-api.team-mulighetsrommet",
            "VALP_SCOPE" to "api://dev-gcp.team-mulighetsrommet.mulighetsrommet-api/.default",
            "ARENA_URL" to "https://tiltakspenger-arena.dev-fss-pub.nais.io", // TODO finn riktig verdi
            "ARENA_SCOPE" to "api://dev-fss.tpts.tiltakspenger-arena/.default", // TODO hvis vi trenger denne må vi finne riktig verdi
            "TILTAK_URL" to "http://team-tiltak", // TODO finn riktig verdi
            "TILTAK_SCOPE" to "api://dev-gcp.team-tiltak.todo/.default", // TODO hvis vi trenger denne må vi finne riktig verdi
        ),
    )
    private val prodProperties = ConfigurationMap(
        mapOf(
            "application.profile" to Profile.PROD.toString(),
            "logback.configurationFile" to "logback.xml",
            "KOMET_URL" to "http://amt-tiltak.amt",
            "KOMET_SCOPE" to "api://prod-gcp.amt.amt-tiltak/.default",
            "VALP_URL" to "http://mulighetsrommet-api.team-mulighetsrommet",
            "VALP_SCOPE" to "api://prod-gcp.team-mulighetsrommet.mulighetsrommet-api/.default",
            "ARENA_URL" to "https://tiltakspenger-arena.prod-fss-pub.nais.io", // TODO finn riktig verdi
            "ARENA_SCOPE" to "api://prod-fss.tpts.tiltakspenger-arena/.default", // TODO hvis vi trenger denne må vi finne riktig verdi
            "TILTAK_URL" to "http://team-tiltak", // TODO finn riktig verdi
            "TILTAK_SCOPE" to "api://prod-gcp.team-tiltak.todo/.default", // TODO hvis vi trenger denne må vi finne riktig verdi
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

    fun kjøreMiljø() = when (System.getenv("NAIS_CLUSTER_NAME") ?: System.getProperty("NAIS_CLUSTER_NAME")) {
        "dev-gcp" -> Profile.DEV
        "prod-gcp" -> Profile.PROD
        else -> Profile.LOCAL
    }

    fun logbackConfigurationFile() = config()[Key("logback.configurationFile", stringType)]

    fun kometClientConfig(baseUrl: String = config()[Key("KOMET_URL", stringType)]) =
        ClientConfig(baseUrl = baseUrl)

    fun valpClientConfig(baseUrl: String = config()[Key("VALP_URL", stringType)]) =
        ClientConfig(baseUrl = baseUrl)

    fun arenaClientConfig(baseUrl: String = config()[Key("ARENA_URL", stringType)]) =
        ClientConfig(baseUrl = baseUrl)

    fun tiltakClientConfig(baseUrl: String = config()[Key("TILTAK_URL", stringType)]) =
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

    fun oauthConfigValp(
        scope: String = config()[Key("VALP_SCOPE", stringType)],
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

    fun oauthConfigTiltak(
        scope: String = config()[Key("TILTAK_SCOPE", stringType)],
        clientId: String = config()[Key("AZURE_APP_CLIENT_ID", stringType)],
        clientSecret: String = config()[Key("AZURE_APP_CLIENT_SECRET", stringType)],
        wellknownUrl: String = config()[Key("AZURE_APP_WELL_KNOWN_URL", stringType)],
    ) = AzureTokenProvider.OauthConfig(
        scope = scope,
        clientId = clientId,
        clientSecret = clientSecret,
        wellknownUrl = wellknownUrl,
    )

//    data class TokenVerificationConfig(
//        val jwksUri: String = config()[Key("AZURE_OPENID_CONFIG_JWKS_URI", stringType)],
//        val issuer: String = config()[Key("AZURE_OPENID_CONFIG_ISSUER", stringType)],
//        val clientId: String = config()[Key("AZURE_APP_CLIENT_ID", stringType)],
//        val leeway: Long = 1000,
//    )
}
