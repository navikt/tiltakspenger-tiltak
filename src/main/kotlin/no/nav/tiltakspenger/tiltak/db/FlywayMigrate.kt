package no.nav.tiltakspenger.tiltak.db

import no.nav.tiltakspenger.tiltak.Configuration
import org.flywaydb.core.Flyway
import javax.sql.DataSource

private fun flyway(dataSource: DataSource): Flyway =
    if (Configuration.isNais()) {
        gcpFlyway(dataSource)
    } else {
        localFlyway(dataSource)
    }

private fun localFlyway(dataSource: DataSource) =
    Flyway
        .configure()
        .loggers("slf4j")
        .encoding("UTF-8")
        .locations("db/migration", "db/local-migration")
        .dataSource(dataSource)
        .load()

private fun gcpFlyway(dataSource: DataSource) =
    Flyway
        .configure()
        .loggers("slf4j")
        .encoding("UTF-8")
        .dataSource(dataSource)
        .load()

fun flywayMigrate(dataSource: DataSource) {
    flyway(dataSource).migrate()
}
