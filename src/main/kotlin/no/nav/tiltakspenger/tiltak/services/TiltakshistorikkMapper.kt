package no.nav.tiltakspenger.tiltak.services

import no.nav.tiltakspenger.libs.tiltak.TiltakResponsDTO.TiltakTypeDTO
import no.nav.tiltakspenger.libs.tiltak.TiltakshistorikkDTO
import no.nav.tiltakspenger.tiltak.clients.tiltakshistorikk.dto.TiltakshistorikkV1Dto
import no.nav.tiltakspenger.tiltak.clients.tiltakshistorikk.dto.toArenaKode
import no.nav.tiltakspenger.tiltak.clients.tiltakshistorikk.dto.toDeltakerStatusDTO

fun TiltakshistorikkV1Dto.TeamKometDeltakelse.toTiltakshistorikkTilSaksbehandlingDTO(): TiltakshistorikkDTO {
    return TiltakshistorikkDTO(
        id = id.toString(),
        gjennomforing = TiltakshistorikkDTO.GjennomforingDTO(
            id = gjennomforing.id.toString(),
            visningsnavn = tittel,
            arrangornavn = arrangor.hovedenhet?.navn ?: arrangor.underenhet.navn,
            typeNavn = tiltakstype.navn,
            arenaKode = tiltakstype.tiltakskode.toArenaKode(),
            deltidsprosent = gjennomforing.deltidsprosent?.toDouble(),
        ),
        deltakelseFom = startDato,
        deltakelseTom = sluttDato,
        deltakelseStatus = status.toDeltakerStatusDTO(),
        antallDagerPerUke = dagerPerUke,
        deltakelseProsent = deltidsprosent,
        kilde = TiltakshistorikkDTO.Kilde.KOMET,
    )
}

fun TiltakshistorikkV1Dto.ArenaDeltakelse.toTiltakshistorikkTilSaksbehandlingDTO(): TiltakshistorikkDTO {
    return TiltakshistorikkDTO(
        id = "TA$arenaId",
        gjennomforing = TiltakshistorikkDTO.GjennomforingDTO(
            id = "",
            visningsnavn = tittel,
            arrangornavn = arrangor.hovedenhet?.navn ?: arrangor.underenhet.navn,
            typeNavn = tiltakstype.navn,
            arenaKode = TiltakTypeDTO.valueOf(tiltakstype.tiltakskode),
            deltidsprosent = null,
        ),
        deltakelseFom = startDato,
        deltakelseTom = sluttDato,
        deltakelseStatus = status.toDeltakerStatusDTO(startDato),
        antallDagerPerUke = dagerPerUke,
        deltakelseProsent = deltidsprosent,
        kilde = TiltakshistorikkDTO.Kilde.ARENA,
    )
}

fun TiltakshistorikkV1Dto.TeamTiltakAvtale.toTiltakshistorikkTilSaksbehandlingDTO(): TiltakshistorikkDTO {
    return TiltakshistorikkDTO(
        id = id.toString(),
        gjennomforing = TiltakshistorikkDTO.GjennomforingDTO(
            id = "",
            visningsnavn = tittel,
            arrangornavn = arbeidsgiver.navn,
            typeNavn = tiltakstype.navn,
            arenaKode = tiltakstype.tiltakskode.toArenaKode(),
            deltidsprosent = null,
        ),
        deltakelseFom = startDato,
        deltakelseTom = sluttDato,
        deltakelseStatus = status.toDeltakerStatusDTO(),
        antallDagerPerUke = dagerPerUke,
        deltakelseProsent = stillingsprosent,
        kilde = TiltakshistorikkDTO.Kilde.TEAM_TILTAK,
    )
}

fun TiltakshistorikkV1Dto.TeamTiltakAvtale.Tiltakskode.toArenaKode(): TiltakTypeDTO =
    when (this) {
        TiltakshistorikkV1Dto.TeamTiltakAvtale.Tiltakskode.ARBEIDSTRENING -> TiltakTypeDTO.ARBTREN
        TiltakshistorikkV1Dto.TeamTiltakAvtale.Tiltakskode.MIDLERTIDIG_LONNSTILSKUDD -> TiltakTypeDTO.MIDLONTIL
        TiltakshistorikkV1Dto.TeamTiltakAvtale.Tiltakskode.VARIG_LONNSTILSKUDD -> TiltakTypeDTO.VARLONTIL
        TiltakshistorikkV1Dto.TeamTiltakAvtale.Tiltakskode.MENTOR -> TiltakTypeDTO.MENTOR
        TiltakshistorikkV1Dto.TeamTiltakAvtale.Tiltakskode.INKLUDERINGSTILSKUDD -> TiltakTypeDTO.INKLUTILS
        TiltakshistorikkV1Dto.TeamTiltakAvtale.Tiltakskode.SOMMERJOBB -> TiltakTypeDTO.TILSJOBB
        TiltakshistorikkV1Dto.TeamTiltakAvtale.Tiltakskode.VTAO -> TiltakTypeDTO.VATIAROR
        TiltakshistorikkV1Dto.TeamTiltakAvtale.Tiltakskode.FIREARIG_LONNSTILSKUDD -> TiltakTypeDTO.FIREARIG_LONNSTILSKUDD
    }
