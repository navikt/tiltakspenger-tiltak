package no.nav.tiltakspenger.tiltak.services

import no.nav.tiltakspenger.libs.tiltak.TiltakResponsDTO.TiltakType
import no.nav.tiltakspenger.tiltak.clients.tiltakshistorikk.dto.TiltakshistorikkV1Dto
import no.nav.tiltakspenger.tiltak.clients.tiltakshistorikk.dto.toArenaKode
import no.nav.tiltakspenger.tiltak.clients.tiltakshistorikk.dto.toDeltakerStatusDTO
import no.nav.tiltakspenger.tiltak.routes.TiltakshistorikkDTO

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
        deltakelsePerUke = dagerPerUke,
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
        deltakelsePerUke = dagerPerUke,
        deltakelseProsent = deltidsprosent,
        kilde = TiltakshistorikkDTO.Kilde.ARENA,
    )
}
