package no.nav.tiltakspenger.tiltak

import mu.KotlinLogging

fun main() {
    System.setProperty("logback.configurationFile", Configuration.logbackConfigurationFile())

    val log = KotlinLogging.logger {}
    val securelog = KotlinLogging.logger("tjenestekall")
    log.info { "starting server" }
    Thread.setDefaultUncaughtExceptionHandler { _, e ->
        log.error { e }
        securelog.error(e) { e.message }
    }

    val applicationBuilder = ApplicationBuilder()
    applicationBuilder.start()
}
