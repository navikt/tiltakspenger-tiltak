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

    private val otherDefaultProperties = mapOf(
        "application.httpPort" to 8080.toString(),
//        "SERVICEUSER_TPTS_USERNAME" to System.getenv("SERVICEUSER_TPTS_USERNAME"),
//        "SERVICEUSER_TPTS_PASSWORD" to System.getenv("SERVICEUSER_TPTS_PASSWORD"),
        "AZURE_APP_CLIENT_ID" to System.getenv("AZURE_APP_CLIENT_ID"),
        "AZURE_APP_CLIENT_SECRET" to System.getenv("AZURE_APP_CLIENT_SECRET"),
        "AZURE_APP_WELL_KNOWN_URL" to System.getenv("AZURE_APP_WELL_KNOWN_URL"),
        "logback.configurationFile" to "logback.xml",
    )

    private val defaultProperties = ConfigurationMap(otherDefaultProperties)

    private val localProperties = ConfigurationMap(
        mapOf(
            "application.profile" to Profile.LOCAL.toString(),
            "logback.configurationFile" to "logback.local.xml",
            "KOMET_URL" to "http://localhost", // TODO her kan vi legge inn stubbing?
            "KOMET_SCOPE" to "api://localhost/.default", // TODO her kan vi legge inn stubbing?
            "AZURE_APP_CLIENT_ID" to "xxx",
            "AZURE_APP_CLIENT_SECRET" to "YYY",
            "AZURE_APP_WELL_KNOWN_URL" to "ZZZ",
        ),
    )
    private val devProperties = ConfigurationMap(
        mapOf(
            "application.profile" to Profile.DEV.toString(),
            "logback.configurationFile" to "logback.xml",
            "KOMET_URL" to "http://amt-tiltak.amt",
            "KOMET_SCOPE" to "api://dev-gcp.amt.amt-tiltak/.default",
            "VALP_URL" to "https://valp.intern.dev.nav.no",
            "VALP_SCOPE" to "api://dev-gcp.team-valp.valp-app/.default",
        ),
    )
    private val prodProperties = ConfigurationMap(
        mapOf(
            "application.profile" to Profile.PROD.toString(),
            "logback.configurationFile" to "logback.xml",
            "KOMET_URL" to "todo", // TODO Vi må finne den riktige.
            "KOMET_SCOPE" to "todo", // TODO Vi må finne den riktige
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
        KometClientConfig(baseUrl = baseUrl)

    data class KometClientConfig(
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

//    data class TokenVerificationConfig(
//        val jwksUri: String = config()[Key("AZURE_OPENID_CONFIG_JWKS_URI", stringType)],
//        val issuer: String = config()[Key("AZURE_OPENID_CONFIG_ISSUER", stringType)],
//        val clientId: String = config()[Key("AZURE_APP_CLIENT_ID", stringType)],
//        val leeway: Long = 1000,
//    )
}
