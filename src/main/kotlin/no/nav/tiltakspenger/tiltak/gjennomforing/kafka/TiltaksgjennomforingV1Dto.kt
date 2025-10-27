package no.nav.tiltakspenger.tiltak.gjennomforing.kafka

import no.nav.tiltakspenger.tiltak.gjennomforing.db.Gjennomforing
import java.util.UUID

data class TiltaksgjennomforingV1Dto(
    val id: UUID,
    val tiltakstype: Tiltakstype,
    val deltidsprosent: Double?,
) {
    data class Tiltakstype(
        val id: UUID,
    )

    fun toGjennomforing() = Gjennomforing(
        id = id,
        tiltakstypeId = tiltakstype.id,
        deltidsprosent = deltidsprosent,
    )
}
