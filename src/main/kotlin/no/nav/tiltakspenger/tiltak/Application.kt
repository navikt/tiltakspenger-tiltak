package no.nav.tiltakspenger.tiltak

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.ktor.common.oppstart.startApp
import no.nav.tiltakspenger.libs.tid.zoneIdOslo
import java.time.Clock

// Testendring for å verifisere fork-PR-gaten i de delte workflowene - PR-en skal lukkes umerget.
fun main() {
    System.setProperty("logback.configurationFile", Configuration.logbackConfigurationFile())

    val log = KotlinLogging.logger {}
    log.info { "starting server" }
    start(log, clock = Clock.system(zoneIdOslo))
}

fun start(
    log: KLogger,
    port: Int = Configuration.httpPort(),
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
        isNais = isNais,
    ) { readiness ->
        ktorSetup(
            texasClient = applicationContext.texasClient,
            tiltakshistorikkService = applicationContext.tiltakshistorikkService,
            readiness = readiness,
        )
    }
}
