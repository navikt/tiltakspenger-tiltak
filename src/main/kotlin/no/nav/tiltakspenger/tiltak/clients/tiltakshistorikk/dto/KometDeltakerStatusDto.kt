package no.nav.tiltakspenger.tiltak.clients.tiltakshistorikk.dto

import no.nav.tiltakspenger.libs.tiltak.TiltakResponsDTO.DeltakerStatusDTO

data class KometDeltakerStatusDto(
    val type: DeltakerStatusType,
) {
    enum class DeltakerStatusType {
        AVBRUTT,
        AVBRUTT_UTKAST,
        DELTAR,
        FEILREGISTRERT,
        FULLFORT,
        HAR_SLUTTET,
        IKKE_AKTUELL,
        KLADD,
        PABEGYNT_REGISTRERING,
        SOKT_INN,
        UTKAST_TIL_PAMELDING,
        VENTELISTE,
        VENTER_PA_OPPSTART,
        VURDERES,
    }

    fun toDeltakerStatusDTO(): DeltakerStatusDTO = when (type) {
        DeltakerStatusType.UTKAST_TIL_PAMELDING,
        DeltakerStatusType.PABEGYNT_REGISTRERING,
        -> DeltakerStatusDTO.PABEGYNT_REGISTRERING

        DeltakerStatusType.AVBRUTT_UTKAST,
        DeltakerStatusType.IKKE_AKTUELL,
        -> DeltakerStatusDTO.IKKE_AKTUELL

        DeltakerStatusType.VENTER_PA_OPPSTART -> DeltakerStatusDTO.VENTER_PA_OPPSTART
        DeltakerStatusType.DELTAR -> DeltakerStatusDTO.DELTAR
        DeltakerStatusType.HAR_SLUTTET -> DeltakerStatusDTO.HAR_SLUTTET
        DeltakerStatusType.FEILREGISTRERT -> DeltakerStatusDTO.FEILREGISTRERT
        DeltakerStatusType.SOKT_INN -> DeltakerStatusDTO.SOKT_INN
        DeltakerStatusType.VURDERES -> DeltakerStatusDTO.VURDERES
        DeltakerStatusType.VENTELISTE -> DeltakerStatusDTO.VENTELISTE
        DeltakerStatusType.AVBRUTT -> DeltakerStatusDTO.AVBRUTT
        DeltakerStatusType.FULLFORT -> DeltakerStatusDTO.FULLFORT

        DeltakerStatusType.KLADD -> throw IllegalArgumentException("Kan ikke mappe kladd til intern status")
    }
}
