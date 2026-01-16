package no.nav.tiltakspenger.tiltak.clients.tiltakshistorikk.dto

import no.nav.tiltakspenger.libs.tiltak.TiltakResponsDTO

enum class TiltakskodeDto {
    ARBEIDSFORBEREDENDE_TRENING,
    ARBEIDSRETTET_REHABILITERING,
    AVKLARING,
    DIGITALT_OPPFOLGINGSTILTAK,
    ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING,
    ENKELTPLASS_FAG_OG_YRKESOPPLAERING,
    GRUPPE_ARBEIDSMARKEDSOPPLAERING,
    GRUPPE_FAG_OG_YRKESOPPLAERING,
    HOYERE_UTDANNING,
    JOBBKLUBB,
    OPPFOLGING,
    VARIG_TILRETTELAGT_ARBEID_SKJERMET,

    ARBEIDSMARKEDSOPPLAERING,
    NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV,
    STUDIESPESIALISERING,
    FAG_OG_YRKESOPPLAERING,
    HOYERE_YRKESFAGLIG_UTDANNING,
}

fun TiltakskodeDto.toArenaKode(): TiltakResponsDTO.TiltakType {
    return when (this) {
        TiltakskodeDto.ARBEIDSFORBEREDENDE_TRENING -> TiltakResponsDTO.TiltakType.ARBFORB
        TiltakskodeDto.ARBEIDSRETTET_REHABILITERING -> TiltakResponsDTO.TiltakType.ARBRRHDAG
        TiltakskodeDto.AVKLARING -> TiltakResponsDTO.TiltakType.AVKLARAG
        TiltakskodeDto.DIGITALT_OPPFOLGINGSTILTAK -> TiltakResponsDTO.TiltakType.DIGIOPPARB
        TiltakskodeDto.GRUPPE_ARBEIDSMARKEDSOPPLAERING,
        TiltakskodeDto.ARBEIDSMARKEDSOPPLAERING,
        TiltakskodeDto.NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV,
        TiltakskodeDto.STUDIESPESIALISERING,
        -> TiltakResponsDTO.TiltakType.GRUPPEAMO

        TiltakskodeDto.GRUPPE_FAG_OG_YRKESOPPLAERING,
        TiltakskodeDto.FAG_OG_YRKESOPPLAERING,
        TiltakskodeDto.HOYERE_YRKESFAGLIG_UTDANNING,
        -> TiltakResponsDTO.TiltakType.GRUFAGYRKE

        TiltakskodeDto.JOBBKLUBB -> TiltakResponsDTO.TiltakType.JOBBK
        TiltakskodeDto.OPPFOLGING -> TiltakResponsDTO.TiltakType.INDOPPFAG
        TiltakskodeDto.VARIG_TILRETTELAGT_ARBEID_SKJERMET -> TiltakResponsDTO.TiltakType.VASV
        TiltakskodeDto.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING -> TiltakResponsDTO.TiltakType.ENKELAMO
        TiltakskodeDto.ENKELTPLASS_FAG_OG_YRKESOPPLAERING -> TiltakResponsDTO.TiltakType.ENKFAGYRKE
        TiltakskodeDto.HOYERE_UTDANNING -> TiltakResponsDTO.TiltakType.HOYEREUTD
    }
}
