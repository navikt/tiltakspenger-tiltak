package no.nav.tiltakspenger.tiltak.services

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

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
