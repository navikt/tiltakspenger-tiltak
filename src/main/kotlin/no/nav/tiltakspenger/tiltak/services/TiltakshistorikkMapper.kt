package no.nav.tiltakspenger.tiltak.services

import no.nav.tiltakspenger.libs.tiltak.TiltakResponsDTO.TiltakType
import no.nav.tiltakspenger.tiltak.clients.tiltakshistorikk.dto.TiltakshistorikkV1Dto
import no.nav.tiltakspenger.tiltak.clients.tiltakshistorikk.dto.toArenaKode
import no.nav.tiltakspenger.tiltak.clients.tiltakshistorikk.dto.toDeltakerStatusDTO
import no.nav.tiltakspenger.tiltak.routes.TiltakshistorikkTilSaksbehandlingDTO

fun TiltakshistorikkV1Dto.TeamKometDeltakelse.toTiltakshistorikkTilSaksbehandlingDTO(): TiltakshistorikkTilSaksbehandlingDTO {
    return TiltakshistorikkTilSaksbehandlingDTO(
        id = id.toString(),
        gjennomforing = TiltakshistorikkTilSaksbehandlingDTO.GjennomforingDTO(
            id = gjennomforing.id.toString(),
            visningsnavn = tittel,
            typeNavn = tiltakstype.navn,
            arenaKode = tiltakstype.tiltakskode.toArenaKode(),
            deltidsprosent = gjennomforing.deltidsprosent?.toDouble(),
        ),
        deltakelseFom = startDato,
        deltakelseTom = sluttDato,
        deltakelseStatus = status.toDeltakerStatusDTO(),
        deltakelsePerUke = dagerPerUke,
        deltakelseProsent = deltidsprosent,
        kilde = "Komet",
    )
}

fun TiltakshistorikkV1Dto.ArenaDeltakelse.toTiltakshistorikkTilSaksbehandlingDTO(): TiltakshistorikkTilSaksbehandlingDTO {
    return TiltakshistorikkTilSaksbehandlingDTO(
        id = "TA$arenaId",
        gjennomforing = TiltakshistorikkTilSaksbehandlingDTO.GjennomforingDTO(
            id = "",
            visningsnavn = tittel,
            typeNavn = tiltakstype.navn,
            arenaKode = TiltakType.valueOf(tiltakstype.tiltakskode),
            deltidsprosent = null,
        ),
        deltakelseFom = startDato,
        deltakelseTom = sluttDato,
        deltakelseStatus = status.toDeltakerStatusDTO(startDato),
        deltakelsePerUke = dagerPerUke,
        deltakelseProsent = deltidsprosent,
        kilde = "Arena",
    )
}
