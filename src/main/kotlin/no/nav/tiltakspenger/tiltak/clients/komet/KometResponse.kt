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
    // Arena type
    val type: String,
    val tiltakstypeNavn: String,
    val arrangor: ArrangorDTO,
)

/**
 * https://confluence.adeo.no/pages/viewpage.action?pageId=573710206
 * https://confluence.adeo.no/pages/viewpage.action?pageId=597205082
 */
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

    /** Mappes til AKTUELL i Arena */
    UTKAST_TIL_PAMELDING,

    /** Mappes til IKKAKTUELL i Arena */
    AVBRUTT_UTKAST,
}
