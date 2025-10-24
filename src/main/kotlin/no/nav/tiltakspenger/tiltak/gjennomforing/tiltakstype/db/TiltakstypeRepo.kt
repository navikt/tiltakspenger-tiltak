package no.nav.tiltakspenger.tiltak.gjennomforing.tiltakstype.db

import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.Row
import kotliquery.queryOf
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.sqlQuery
import no.nav.tiltakspenger.libs.tiltak.TiltakResponsDTO
import java.time.LocalDateTime
import java.util.UUID

class TiltakstypeRepo(
    private val sessionFactory: PostgresSessionFactory,
) {
    fun lagre(tiltakstype: Tiltakstype) {
        sessionFactory.withTransaction { session ->
            session.run(
                sqlQuery(
                    """
                    insert into tiltakstype (
                        id,
                        navn,
                        tiltakskode,
                        arenakode,
                        sist_endret
                    ) values (
                        :id,
                        :navn,
                        :tiltakskode,
                        :arenakode,
                        :sist_endret
                    )
                    on conflict (id) do update set
                        navn = :navn,
                        tiltakskode = :tiltakskode,
                        arenakode = :arenakode,
                        sist_endret = :sist_endret
                """,
                    "id" to tiltakstype.id,
                    "navn" to tiltakstype.navn,
                    "tiltakskode" to tiltakstype.tiltakskode.name,
                    "arenakode" to tiltakstype.arenakode?.name,
                    "sist_endret" to LocalDateTime.now(),
                ).asUpdate,
            )
        }
    }

    fun hent(
        id: UUID,
    ): Tiltakstype? {
        return sessionFactory.withSession { session ->
            session.run(
                queryOf(
                    """
                    select * from tiltakstype
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

    private fun fromRow(row: Row): Tiltakstype = Tiltakstype(
        id = row.uuid("id"),
        navn = row.string("navn"),
        tiltakskode = Tiltakskode.valueOf(row.string("tiltakskode")),
        arenakode = row.stringOrNull("arenakode")?.let { TiltakResponsDTO.TiltakType.valueOf(it) },
    )
}
