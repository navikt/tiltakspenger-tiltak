package no.nav.tiltakspenger.tiltak.gjennomforing.tiltakstype.kafka

import no.nav.tiltakspenger.libs.tiltak.TiltakResponsDTO
import no.nav.tiltakspenger.tiltak.gjennomforing.tiltakstype.db.Tiltakskode
import no.nav.tiltakspenger.tiltak.gjennomforing.tiltakstype.db.Tiltakstype
import java.util.UUID

data class TiltakstypeDto(
    val id: UUID,
    val navn: String,
    val tiltakskode: Tiltakskode,
    val arenaKode: String?,
) {
    fun toTiltakstype(): Tiltakstype {
        return Tiltakstype(
            id = id,
            navn = navn,
            tiltakskode = tiltakskode,
            arenakode = arenaKode?.let { TiltakResponsDTO.TiltakType.valueOf(it) } ?: tiltakskode.toArenaKode(),
        )
    }
}
