package no.nav.tiltakspenger.tiltak.services

import no.nav.tiltakspenger.libs.tiltak.TiltakResponsDTO.TiltakType
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
            arenaKode = TiltakType.valueOf(tiltakstype.tiltakskode),
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

fun TiltakshistorikkV1Dto.TeamTiltakAvtale.Tiltakskode.toArenaKode(): TiltakType =
    when (this) {
        TiltakshistorikkV1Dto.TeamTiltakAvtale.Tiltakskode.ARBEIDSTRENING -> TiltakType.ARBTREN
        TiltakshistorikkV1Dto.TeamTiltakAvtale.Tiltakskode.MIDLERTIDIG_LONNSTILSKUDD -> TiltakType.MIDLONTIL
        TiltakshistorikkV1Dto.TeamTiltakAvtale.Tiltakskode.VARIG_LONNSTILSKUDD -> TiltakType.VARLONTIL
        TiltakshistorikkV1Dto.TeamTiltakAvtale.Tiltakskode.MENTOR -> TiltakType.MENTOR
        TiltakshistorikkV1Dto.TeamTiltakAvtale.Tiltakskode.INKLUDERINGSTILSKUDD -> TiltakType.INKLUTILS
        TiltakshistorikkV1Dto.TeamTiltakAvtale.Tiltakskode.SOMMERJOBB -> TiltakType.TILSJOBB
        TiltakshistorikkV1Dto.TeamTiltakAvtale.Tiltakskode.VTAO -> TiltakType.VATIAROR
    }
