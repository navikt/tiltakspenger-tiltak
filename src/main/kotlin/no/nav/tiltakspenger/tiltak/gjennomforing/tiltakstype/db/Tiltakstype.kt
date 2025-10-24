package no.nav.tiltakspenger.tiltak.gjennomforing.tiltakstype.db

import no.nav.tiltakspenger.libs.tiltak.TiltakResponsDTO.TiltakType
import java.util.UUID

data class Tiltakstype(
    val id: UUID,
    val navn: String,
    val tiltakskode: Tiltakskode,
    val arenakode: TiltakType?,
)
