package no.nav.tiltakspenger.tiltak

import com.natpryce.konfig.ConfigurationMap
import com.natpryce.konfig.ConfigurationProperties.Companion.systemProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.Key
import com.natpryce.konfig.overriding
import com.natpryce.konfig.stringType

enum class Profile {
    LOCAL,
    DEV,
    PROD,
}

object Configuration {

    private val otherDefaultProperties = mapOf(
        "application.httpPort" to 8080.toString(),
        "logback.configurationFile" to "logback.xml",
        "NAIS_TOKEN_ENDPOINT" to System.getenv("NAIS_TOKEN_ENDPOINT"),
        "NAIS_TOKEN_INTROSPECTION_ENDPOINT" to System.getenv("NAIS_TOKEN_INTROSPECTION_ENDPOINT"),
        "NAIS_TOKEN_EXCHANGE_ENDPOINT" to System.getenv("NAIS_TOKEN_EXCHANGE_ENDPOINT"),
        "DB_JDBC_URL" to System.getenv("DB_JDBC_URL"),
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
            "KOMET_TESTDATA_URL" to "http://localhost",
            "KOMET_TESTDATA_SCOPE" to "api://localhost/.default",
            "NAIS_TOKEN_ENDPOINT" to "http://localhost",
            "NAIS_TOKEN_INTROSPECTION_ENDPOINT" to "http://localhost",
            "NAIS_TOKEN_EXCHANGE_ENDPOINT" to "http://localhost",
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

    fun isNais() = applicationProfile() != Profile.LOCAL

    fun logbackConfigurationFile() = config()[Key("logback.configurationFile", stringType)]

    val naisTokenIntrospectionEndpoint: String = config()[Key("NAIS_TOKEN_INTROSPECTION_ENDPOINT", stringType)]
    val naisTokenEndpoint: String = config()[Key("NAIS_TOKEN_ENDPOINT", stringType)]
    val tokenExchangeEndpoint: String = config()[Key("NAIS_TOKEN_EXCHANGE_ENDPOINT", stringType)]

    val kometUrl: String = config()[Key("KOMET_URL", stringType)]
    val kometScope: String = config()[Key("KOMET_SCOPE", stringType)]
    val arenaUrl: String = config()[Key("ARENA_URL", stringType)]
    val arenaScope: String = config()[Key("ARENA_SCOPE", stringType)]
    val kometTestdataUrl = config()[Key("KOMET_TESTDATA_URL", stringType)]
    val kometTestdataScope: String = config()[Key("KOMET_TESTDATA_SCOPE", stringType)]

    val jdbcUrl: String = config()[Key("DB_JDBC_URL", stringType)]
}
