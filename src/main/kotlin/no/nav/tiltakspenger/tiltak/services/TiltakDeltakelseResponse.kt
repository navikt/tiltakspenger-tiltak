package no.nav.tiltakspenger.tiltak.services

import no.nav.tiltakspenger.libs.arena.tiltak.ArenaTiltaksaktivitetResponsDTO
import no.nav.tiltakspenger.tiltak.clients.komet.DeltakerStatusDTO
import java.time.LocalDate
import java.time.LocalDateTime

// TODO Dette er svaret vi leverer ut igjen fra denne appen.
//      Denne returnerer alle data vi får tilbake fra Komet + alle data vi får tilbake fra valp slik den er nå
//      Her må vi finne ut hvilke data vi trenger og endre denne til et format vi er happy med
//      I tillegg må data fra Tiltak som vi ikke kaller ende med i denne
data class TiltakDeltakelseResponse(
    val id: String,
    val gjennomforing: GjennomforingResponseDTO,
    val startDato: LocalDate?,
    val sluttDato: LocalDate?,
    val status: DeltakerStatusResponseDTO,
    val dagerPerUke: Float?,
    val prosentStilling: Float?,
    val registrertDato: LocalDateTime,
)

data class GjennomforingResponseDTO(
    val id: String,
    val arrangornavn: String,
    val typeNavn: String,
    val arenaKode: String,
//    val status: TiltaksgjennomforingsstatusResponse,
    val startDato: LocalDate?,
    val sluttDato: LocalDate?,
)

enum class DeltakerStatusResponseDTO {
    VENTER_PA_OPPSTART,
    DELTAR,
    HAR_SLUTTET,
    IKKE_AKTUELL,
    FEILREGISTRERT,
    PABEGYNT_REGISTRERING,
    SOKT_INN,
    VENTELISTE,
    VURDERES,
    AVBRUTT,
    FULLFORT,
}

// data class ValpResponse(
//    val id: UUID,
//    val tiltakstype: TiltakstypeResponse,
//    val navn: String,
//    val startDato: LocalDate,
//    val sluttDato: LocalDate?,
//    val status: TiltaksgjennomforingsstatusResponse,
//    val virksomhetsnummer: String,
//    val oppstart: TiltaksgjennomforingOppstartstypeResponse,
// )
//
// data class TiltakstypeResponse(
//    val id: UUID,
//    val navn: String,
//    val arenaKode: String,
// )

// enum class TiltaksgjennomforingsstatusResponse {
//    GJENNOMFORES,
//    AVBRUTT,
//    AVLYST,
//    AVSLUTTET,
//    APENT_FOR_INNSOK,
// }
//
// enum class TiltaksgjennomforingOppstartstypeResponse {
//    LOPENDE,
//    FELLES,
// }

val kometStatusViVilHa = listOf(
    DeltakerStatusDTO.AVBRUTT,
    DeltakerStatusDTO.FULLFORT,
    DeltakerStatusDTO.DELTAR,
    DeltakerStatusDTO.IKKE_AKTUELL,
    DeltakerStatusDTO.VENTER_PA_OPPSTART,
    DeltakerStatusDTO.HAR_SLUTTET,
)

val tiltakViVilHaFraKomet = setOf(
    "INDOPPFAG",
    "ARBFORB",
    "AVKLARAG",
    // "VASV",  Denne gir ikke rett til tiltakspenger
    "ARBRRHDAG",
    "DIGIOPPARB",
    "JOBBK",
    "GRUPPEAMO",
    "GRUFAGYRKE",
)

val arenaStatusViVilHa = listOf(
    ArenaTiltaksaktivitetResponsDTO.DeltakerStatusType.DELAVB,
    ArenaTiltaksaktivitetResponsDTO.DeltakerStatusType.FULLF,
    ArenaTiltaksaktivitetResponsDTO.DeltakerStatusType.GJENN,
    ArenaTiltaksaktivitetResponsDTO.DeltakerStatusType.GJENN_AVB,
    ArenaTiltaksaktivitetResponsDTO.DeltakerStatusType.IKKEM,
    ArenaTiltaksaktivitetResponsDTO.DeltakerStatusType.JATAKK,
    ArenaTiltaksaktivitetResponsDTO.DeltakerStatusType.TILBUD,
)

val tiltakViVilHaFraArena = setOf(
    "AMBF1",
    "ABOPPF",
    "ABUOPPF",
    "ABIST",
    "ABTBOPPF",
    "AMO",
    "AMOE",
    "AMOB",
    "AMOY",
    "PRAKSORD",
    "PRAKSKJERM",
    "ARBRRHBAG",
    "ARBRRHBSM",
    "ARBRDAGSM",
    "ARBRRDOGN",
    "ARBDOGNSM",
    "ARBTREN",
    "AVKLARUS",
    "AVKLARSP",
    "AVKLARKV",
    "AVKLARSV",
    "ENKELAMO",
    "ENKFAGYRKE",
    "KAT",
    "VALS",
    "FORSAMOENK",
    "FORSFAGENK",
    "FORSHOYUTD",
    "FUNKSJASS",
    "GRUFAGYRKE",
    "HOYEREUTD",
    "INDJOBSTOT",
    "IPSUNG",
    "INDOPPFOLG",
    "INKLUTILS",
    "JOBBKLUBB",
    "JOBBFOKUS",
    "JOBBBONUS",
    "MENTOR",
    "NETTAMO",
    "INDOPPFSP",
    "INDOPPRF",
    "REFINO",
    "SPA",
    "SUPPEMP",
    "TILPERBED",
    "UTDYRK",
    "UTBHLETTPS",
    "UTBHPSLD",
    "UTBHSAMLI",
    "UTVAOONAV",
    "UTVOPPFOPL",
    "OPPLT2AAR",
    "FORSOPPLEV",
    // "INDOPPFAG",  denne henter vi fra komet
    // "ARBFORB",    denne henter vi fra komet
    // "AVKLARAG",   denne henter vi fra komet
    // "ARBRRHDAG",  denne henter vi fra komet
    // "DIGIOPPARB", denne henter vi fra komet
    // "JOBBK",      denne henter vi fra komet
    // "GRUPPEAMO",  denne henter vi fra komet
    // "GRUFAGYRKE"  denne henter vi fra komet
)
