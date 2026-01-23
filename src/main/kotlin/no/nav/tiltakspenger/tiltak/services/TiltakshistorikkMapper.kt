package no.nav.tiltakspenger.tiltak.services

import no.nav.tiltakspenger.libs.arena.tiltak.ArenaTiltaksaktivitetResponsDTO.TiltaksaktivitetDTO
import no.nav.tiltakspenger.libs.arena.tiltak.toDTO
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

// Vi får ikke gjennomføringId eller deltidsprosent fra Arena
// confluenceside for endepunktet vi kaller for å hente Arenatiltak: https://confluence.adeo.no/pages/viewpage.action?pageId=470748287
fun TiltaksaktivitetDTO.toTiltakshistorikkTilSaksbehandlingDTO(): TiltakshistorikkDTO = TiltakshistorikkDTO(
    id = aktivitetId,
    gjennomforing = TiltakshistorikkDTO.GjennomforingDTO(
        id = "",
        visningsnavn = arrangoer?.let { "${tiltakType.navn} hos $it" } ?: "Ukjent",
        arrangornavn = arrangoer ?: "Ukjent",
        typeNavn = tiltakType.navn,
        arenaKode = TiltakType.valueOf(tiltakType.name),
        deltidsprosent = null,
    ),
    deltakelseFom = deltakelsePeriode?.fom,
    deltakelseTom = deltakelsePeriode?.tom,
    deltakelseStatus = deltakerStatusType.toDTO(deltakelsePeriode?.fom),
    antallDagerPerUke = antallDagerPerUke,
    deltakelseProsent = deltakelseProsent,
    kilde = TiltakshistorikkDTO.Kilde.ARENA,
)
