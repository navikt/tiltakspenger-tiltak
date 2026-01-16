package no.nav.tiltakspenger.tiltak.clients.tiltakshistorikk.dto

import no.nav.tiltakspenger.libs.tiltak.TiltakResponsDTO
import java.time.LocalDate

enum class ArenaDeltakerStatusDto {
    AKTUELL,
    AVSLAG,
    DELTAKELSE_AVBRUTT,
    FEILREGISTRERT,
    FULLFORT,
    GJENNOMFORES,
    GJENNOMFORING_AVBRUTT,
    GJENNOMFORING_AVLYST,
    IKKE_AKTUELL,
    IKKE_MOTT,
    INFORMASJONSMOTE,
    TAKKET_JA_TIL_TILBUD,
    TAKKET_NEI_TIL_TILBUD,
    TILBUD,
    VENTELISTE,
}

fun ArenaDeltakerStatusDto.toDeltakerStatusDTO(fom: LocalDate?): TiltakResponsDTO.DeltakerStatusDTO {
    val startdatoErFremITid = fom == null || fom.isAfter(LocalDate.now())

    return when (this) {
        ArenaDeltakerStatusDto.DELTAKELSE_AVBRUTT -> TiltakResponsDTO.DeltakerStatusDTO.AVBRUTT
        ArenaDeltakerStatusDto.FULLFORT -> TiltakResponsDTO.DeltakerStatusDTO.FULLFORT
        ArenaDeltakerStatusDto.GJENNOMFORES -> if (startdatoErFremITid) TiltakResponsDTO.DeltakerStatusDTO.VENTER_PA_OPPSTART else TiltakResponsDTO.DeltakerStatusDTO.DELTAR
        ArenaDeltakerStatusDto.GJENNOMFORING_AVBRUTT -> TiltakResponsDTO.DeltakerStatusDTO.AVBRUTT
        ArenaDeltakerStatusDto.IKKE_MOTT -> TiltakResponsDTO.DeltakerStatusDTO.AVBRUTT
        ArenaDeltakerStatusDto.TAKKET_JA_TIL_TILBUD -> TiltakResponsDTO.DeltakerStatusDTO.DELTAR
        ArenaDeltakerStatusDto.TILBUD -> TiltakResponsDTO.DeltakerStatusDTO.VENTER_PA_OPPSTART

        ArenaDeltakerStatusDto.AKTUELL -> TiltakResponsDTO.DeltakerStatusDTO.SOKT_INN
        ArenaDeltakerStatusDto.AVSLAG -> TiltakResponsDTO.DeltakerStatusDTO.IKKE_AKTUELL
        ArenaDeltakerStatusDto.GJENNOMFORING_AVLYST -> TiltakResponsDTO.DeltakerStatusDTO.IKKE_AKTUELL
        ArenaDeltakerStatusDto.IKKE_AKTUELL -> TiltakResponsDTO.DeltakerStatusDTO.IKKE_AKTUELL
        ArenaDeltakerStatusDto.INFORMASJONSMOTE -> TiltakResponsDTO.DeltakerStatusDTO.VENTELISTE
        ArenaDeltakerStatusDto.TAKKET_NEI_TIL_TILBUD -> TiltakResponsDTO.DeltakerStatusDTO.IKKE_AKTUELL
        ArenaDeltakerStatusDto.VENTELISTE -> TiltakResponsDTO.DeltakerStatusDTO.VENTELISTE
        ArenaDeltakerStatusDto.FEILREGISTRERT -> TiltakResponsDTO.DeltakerStatusDTO.FEILREGISTRERT
    }
}
