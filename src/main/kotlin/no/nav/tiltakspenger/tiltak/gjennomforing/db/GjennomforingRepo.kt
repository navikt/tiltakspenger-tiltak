package no.nav.tiltakspenger.tiltak.gjennomforing.db

import kotliquery.Row
import kotliquery.queryOf
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.sqlQuery
import java.time.LocalDateTime
import java.util.UUID

class GjennomforingRepo(
    private val sessionFactory: PostgresSessionFactory,
) {
    fun lagre(gjennomforing: Gjennomforing) {
        sessionFactory.withTransaction { session ->
            session.run(
                sqlQuery(
                    """
                    insert into gjennomforing (
                        id,
                        tiltakstype_id,
                        deltidsprosent,
                        sist_endret
                    ) values (
                        :id,
                        :tiltakstype_id,
                        :deltidsprosent,
                        :sist_endret
                    )
                    on conflict (id) do update set
                        tiltakstype_id = :tiltakstype_id,
                        deltidsprosent = :deltidsprosent,
                        sist_endret = :sist_endret
                """,
                    "id" to gjennomforing.id,
                    "tiltakstype_id" to gjennomforing.tiltakstypeId,
                    "deltidsprosent" to gjennomforing.deltidsprosent,
                    "sist_endret" to LocalDateTime.now(),
                ).asUpdate,
            )
        }
    }

    fun slett(id: UUID) {
        sessionFactory.withTransaction { session ->
            session.run(
                sqlQuery(
                    """
                    delete from gjennomforing where id = :id
                """,
                    "id" to id,
                ).asUpdate,
            )
        }
    }

    fun hent(
        id: UUID,
    ): Gjennomforing? {
        return sessionFactory.withSession { session ->
            session.run(
                queryOf(
                    """
                    select * from gjennomforing
                      where id = :id
                    """.trimIndent(),
                    mapOf(
                        "id" to id,
                    ),
                ).map {
                    fromRow(it)
                }.asSingle,
            )
        }
    }

    private fun fromRow(row: Row): Gjennomforing = Gjennomforing(
        id = row.uuid("id"),
        tiltakstypeId = row.uuid("tiltakstype_id"),
        deltidsprosent = row.doubleOrNull("deltidsprosent"),
    )
}
