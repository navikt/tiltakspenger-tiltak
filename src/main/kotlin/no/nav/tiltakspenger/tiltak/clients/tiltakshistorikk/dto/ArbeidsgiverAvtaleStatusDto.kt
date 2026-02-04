package no.nav.tiltakspenger.tiltak.clients.tiltakshistorikk.dto

import no.nav.tiltakspenger.libs.tiltak.TiltakResponsDTO.DeltakerStatusDTO

enum class ArbeidsgiverAvtaleStatusDto {
    PAABEGYNT,
    MANGLER_GODKJENNING,
    KLAR_FOR_OPPSTART,
    GJENNOMFORES,
    AVSLUTTET,
    AVBRUTT,
    ANNULLERT,
}

fun ArbeidsgiverAvtaleStatusDto.toDeltakerStatusDTO(): DeltakerStatusDTO = when (this) {
    ArbeidsgiverAvtaleStatusDto.PAABEGYNT -> DeltakerStatusDTO.PABEGYNT_REGISTRERING
    ArbeidsgiverAvtaleStatusDto.MANGLER_GODKJENNING -> DeltakerStatusDTO.SOKT_INN
    ArbeidsgiverAvtaleStatusDto.KLAR_FOR_OPPSTART -> DeltakerStatusDTO.VENTER_PA_OPPSTART
    ArbeidsgiverAvtaleStatusDto.GJENNOMFORES -> DeltakerStatusDTO.DELTAR
    ArbeidsgiverAvtaleStatusDto.AVSLUTTET -> DeltakerStatusDTO.HAR_SLUTTET
    ArbeidsgiverAvtaleStatusDto.AVBRUTT -> DeltakerStatusDTO.AVBRUTT
    ArbeidsgiverAvtaleStatusDto.ANNULLERT -> DeltakerStatusDTO.IKKE_AKTUELL
}
