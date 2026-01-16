package no.nav.tiltakspenger.tiltak

import com.natpryce.konfig.ConfigurationMap
import com.natpryce.konfig.ConfigurationProperties.Companion.systemProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.Key
import com.natpryce.konfig.overriding
import com.natpryce.konfig.stringType

private const val APPLICATION_NAME = "tiltakspenger-tiltak"
const val KAFKA_CONSUMER_GROUP_ID = "$APPLICATION_NAME-consumer"

enum class Profile {
    LOCAL,
    DEV,
    PROD,
}

object Configuration {
    private val defaultProperties = ConfigurationMap(
        mapOf(
            "application.httpPort" to 8080.toString(),
            "logback.configurationFile" to "logback.xml",
            "NAIS_TOKEN_ENDPOINT" to System.getenv("NAIS_TOKEN_ENDPOINT"),
            "NAIS_TOKEN_INTROSPECTION_ENDPOINT" to System.getenv("NAIS_TOKEN_INTROSPECTION_ENDPOINT"),
            "NAIS_TOKEN_EXCHANGE_ENDPOINT" to System.getenv("NAIS_TOKEN_EXCHANGE_ENDPOINT"),
            "DB_JDBC_URL" to System.getenv("DB_JDBC_URL"),
            "KOMET_URL" to "http://amt-deltaker.amt",
            "KOMET_SCOPE" to System.getenv("KOMET_SCOPE"),
            "ARENA_URL" to System.getenv("ARENA_URL"),
            "ARENA_SCOPE" to System.getenv("ARENA_SCOPE"),
            "TILTAKSTYPE_TOPIC" to "team-mulighetsrommet.siste-tiltakstyper-v3",
            "GJENNOMFORING_TOPIC" to "team-mulighetsrommet.siste-tiltaksgjennomforinger-v1",
            "TILTAKSHISTORIKK_URL" to "http://tiltakshistorikk.team-mulighetsrommet",
            "TILTAKSHISTORIKK_SCOPE" to System.getenv("TILTAKSHISTORIKK_SCOPE"),
        ),
    )

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
            "TILTAKSHISTORIKK_URL" to "http://localhost",
            "TILTAKSHISTORIKK_SCOPE" to "api://localhost/.default",
        ),
    )
    private val devProperties = ConfigurationMap(
        mapOf(
            "application.profile" to Profile.DEV.toString(),
            "KOMET_TESTDATA_URL" to "http://amt-deltaker-bff.amt",
            "KOMET_TESTDATA_SCOPE" to "api://dev-gcp.amt.amt-deltaker-bff/.default",
        ),
    )
    private val prodProperties = ConfigurationMap(
        mapOf(
            "application.profile" to Profile.PROD.toString(),
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

    val tiltakstypeTopic: String = config()[Key("TILTAKSTYPE_TOPIC", stringType)]
    val gjennomforingTopic: String = config()[Key("GJENNOMFORING_TOPIC", stringType)]

    val tiltakshistorikkUrl: String = config()[Key("TILTAKSHISTORIKK_URL", stringType)]
    val tiltakshistorikkScope: String = config()[Key("TILTAKSHISTORIKK_SCOPE", stringType)]

    val jdbcUrl: String = config()[Key("DB_JDBC_URL", stringType)]
}
