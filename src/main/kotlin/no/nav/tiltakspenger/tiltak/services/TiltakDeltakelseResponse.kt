package no.nav.tiltakspenger.tiltak.services

import no.nav.tiltakspenger.libs.arena.tiltak.ArenaTiltaksaktivitetResponsDTO
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

// TODO Dette er svaret vi leverer ut igjen fra denne appen.
//      Denne returnerer alle data vi får tilbake fra Komet + alle data vi får tilbake fra valp slik den er nå
//      Her må vi finne ut hvilke data vi trenger og endre denne til et format vi er happy med
//      I tillegg må data fra Tiltak som vi ikke kaller ende med i denne
data class TiltakDeltakelseResponse(
    val id: UUID,
    val gjennomforing: GjennomforingResponseDTO,
    val startDato: LocalDate?,
    val sluttDato: LocalDate?,
    val status: DeltakerStatusResponseDTO,
    val dagerPerUke: Float?,
    val prosentStilling: Float?,
    val registrertDato: LocalDateTime,
)

data class GjennomforingResponseDTO(
    val id: UUID,
    val navn: String,
    val type: String, // Arena type
    val arrangor: ArrangorResponseDTO,
    val valp: ValpResponse?,
    val arenatiltak: ArenaTiltaksaktivitetResponsDTO?,
)

data class ArrangorResponseDTO(
    val virksomhetsnummer: String,
    val navn: String,
)

enum class DeltakerStatusResponseDTO {
    VENTER_PA_OPPSTART, DELTAR, HAR_SLUTTET, IKKE_AKTUELL, VURDERES, AVBRUTT
}

data class ValpResponse(
    val id: UUID,
    val tiltakstype: TiltakstypeResponse,
    val navn: String,
    val startDato: LocalDate,
    val sluttDato: LocalDate?,
    val status: TiltaksgjennomforingsstatusResponse,
    val virksomhetsnummer: String,
    val oppstart: TiltaksgjennomforingOppstartstypeResponse,
)

data class TiltakstypeResponse(
    val id: UUID,
    val navn: String,
    val arenaKode: String,
)

enum class TiltaksgjennomforingsstatusResponse {
    GJENNOMFORES,
    AVBRUTT,
    AVLYST,
    AVSLUTTET,
    APENT_FOR_INNSOK,
}

enum class TiltaksgjennomforingOppstartstypeResponse {
    LOPENDE,
    FELLES,
}
