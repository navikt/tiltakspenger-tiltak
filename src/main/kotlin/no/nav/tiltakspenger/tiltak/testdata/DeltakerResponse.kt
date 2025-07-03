package no.nav.tiltakspenger.tiltak.testdata

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class DeltakerResponse(
    val id: UUID,
    val navBruker: NavBruker,
    val deltakerliste: Deltakerliste,
    val startdato: LocalDate?,
    val sluttdato: LocalDate?,
    val dagerPerUke: Float?,
    val deltakelsesprosent: Float?,
    val status: DeltakerStatus,
    val sistEndret: LocalDateTime,
) {
    data class NavBruker(
        val personident: String,
    )

    data class Deltakerliste(
        val id: UUID,
        val navn: String,
    )

    data class DeltakerStatus(
        val type: Type,
    ) {
        enum class Type {
            KLADD,
            UTKAST_TIL_PAMELDING,
            AVBRUTT_UTKAST,
            VENTER_PA_OPPSTART,
            DELTAR,
            HAR_SLUTTET,
            IKKE_AKTUELL,
            FEILREGISTRERT,
            SOKT_INN,
            VURDERES,
            VENTELISTE,
            AVBRUTT,
            FULLFORT,
            PABEGYNT_REGISTRERING,
        }
    }
}
