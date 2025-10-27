package no.nav.tiltakspenger.tiltak.gjennomforing.db

import java.util.UUID

data class Gjennomforing(
    val id: UUID,
    val tiltakstypeId: UUID,
    val deltidsprosent: Double?,
)
