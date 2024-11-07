package no.nav.tiltakspenger.tiltak.clients.komet

import java.time.LocalDate
import java.time.LocalDateTime

data class DeltakerDTO(
    val id: String,
    val gjennomforing: GjennomforingDTO,
    val startDato: LocalDate?,
    val sluttDato: LocalDate?,
    val status: DeltakerStatusDTO,
    val dagerPerUke: Float?,
    val prosentStilling: Float?,
    val registrertDato: LocalDateTime,
)

data class ArrangorDTO(
    val virksomhetsnummer: String,
    val navn: String,
)

data class GjennomforingDTO(
    val id: String,
    val navn: String,
    val type: String, // Arena type
    val tiltakstypeNavn: String,
    val arrangor: ArrangorDTO,
)

enum class DeltakerStatusDTO {
    VENTER_PA_OPPSTART,
    DELTAR,
    HAR_SLUTTET,
    IKKE_AKTUELL,
    VURDERES,
    AVBRUTT,
    FULLFORT,
    FEILREGISTRERT,
    PABEGYNT_REGISTRERING,
    SOKT_INN,
    VENTELISTE,
}
