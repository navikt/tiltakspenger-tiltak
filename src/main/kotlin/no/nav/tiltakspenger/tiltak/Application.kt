package no.nav.tiltakspenger.tiltak

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.ktor.common.oppstart.startApp
import no.nav.tiltakspenger.libs.tid.zoneIdOslo
import java.time.Clock

fun main() {
    System.setProperty("logback.configurationFile", Configuration.logbackConfigurationFile())

    val log = KotlinLogging.logger {}
    log.info { "starting server" }
    start(log, clock = Clock.system(zoneIdOslo))
}

fun start(
    log: KLogger,
    port: Int = Configuration.httpPort(),
    host: String = "0.0.0.0",
    isNais: Boolean = Configuration.isNais(),
    clock: Clock,
    applicationContext: ApplicationContext = ApplicationContext(clock),
) {
    Thread.setDefaultUncaughtExceptionHandler { _, e ->
        log.error(e) { e.message }
    }

    startApp(
        log = log,
        port = port,
        host = host,
        isNais = isNais,
    ) { readiness ->
        ktorSetup(
            texasClient = applicationContext.texasClient,
            tiltakshistorikkService = applicationContext.tiltakshistorikkService,
            readiness = readiness,
        )
    }
}
