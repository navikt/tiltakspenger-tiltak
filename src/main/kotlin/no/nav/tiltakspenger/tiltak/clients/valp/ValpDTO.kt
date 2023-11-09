package no.nav.tiltakspenger.tiltak.clients.valp

import java.time.LocalDate
import java.util.*

data class ValpDTO(
    val id: UUID,
    val tiltakstype: Tiltakstype,
    val navn: String,
    val startDato: LocalDate,
    val sluttDato: LocalDate?,
    val status: Tiltaksgjennomforingsstatus,
    val virksomhetsnummer: String,
    val oppstart: TiltaksgjennomforingOppstartstype,
)

data class Tiltakstype(
    val id: UUID,
    val navn: String,
    val arenaKode: String,
)

enum class Tiltaksgjennomforingsstatus {
    GJENNOMFORES,
    AVBRUTT,
    AVLYST,
    AVSLUTTET,
    PLANLAGT,
}

enum class TiltaksgjennomforingOppstartstype {
    LOPENDE,
    FELLES,
}
