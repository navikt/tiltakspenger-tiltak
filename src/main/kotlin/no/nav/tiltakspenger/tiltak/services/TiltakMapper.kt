package no.nav.tiltakspenger.tiltak.services

import no.nav.tiltakspenger.libs.arena.tiltak.ArenaTiltaksaktivitetResponsDTO.TiltaksaktivitetDTO
import no.nav.tiltakspenger.libs.arena.tiltak.toDTO
import no.nav.tiltakspenger.libs.tiltak.TiltakResponsDTO.GjennomføringDTO
import no.nav.tiltakspenger.libs.tiltak.TiltakResponsDTO.TiltakDTO
import no.nav.tiltakspenger.libs.tiltak.TiltakResponsDTO.TiltakType
import no.nav.tiltakspenger.libs.tiltak.TiltakTilSaksbehandlingDTO
import no.nav.tiltakspenger.libs.tiltak.TiltakshistorikkDTO
import java.time.LocalDateTime

// Vi får ikke gjennomføringId eller deltidsprosent fra Arena
// confluenceside for endepunktet vi kaller for å hente Arenatiltak: https://confluence.adeo.no/pages/viewpage.action?pageId=470748287
internal fun TiltaksaktivitetDTO.toSaksbehandlingDTO(): TiltakTilSaksbehandlingDTO = TiltakTilSaksbehandlingDTO(
    id = aktivitetId,
    gjennomforing = GjennomføringDTO(
        id = "",
        arrangørnavn = arrangoer ?: "Ukjent",
        typeNavn = tiltakType.navn,
        arenaKode = TiltakType.valueOf(tiltakType.name),
        deltidsprosent = null,
    ),
    deltakelseFom = deltakelsePeriode?.fom,
    deltakelseTom = deltakelsePeriode?.tom,
    deltakelseStatus = deltakerStatusType.toDTO(deltakelsePeriode?.fom),
    deltakelsePerUke = antallDagerPerUke,
    deltakelseProsent = deltakelseProsent,
    kilde = "Arena",
)

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
    deltakelsePerUke = antallDagerPerUke,
    deltakelseProsent = deltakelseProsent,
    kilde = TiltakshistorikkDTO.Kilde.ARENA,
)

internal fun TiltaksaktivitetDTO.toSøknadTiltak(): TiltakDTO =
    TiltakDTO(
        id = aktivitetId,
        gjennomforing = GjennomføringDTO(
            id = "",
            arrangørnavn = arrangoer ?: "Ukjent",
            typeNavn = tiltakType.navn,
            arenaKode = TiltakType.valueOf(tiltakType.name),
            deltidsprosent = null,
        ),
        deltakelseFom = deltakelsePeriode?.fom,
        deltakelseTom = deltakelsePeriode?.tom,
        deltakelseStatus = deltakerStatusType.toDTO(deltakelsePeriode?.fom),
        deltakelseDagerUke = antallDagerPerUke,
        deltakelseProsent = deltakelseProsent,
        kilde = "Arena",
        registrertDato = statusSistEndret?.let { LocalDateTime.from(it.atStartOfDay()) } ?: LocalDateTime.now(),
    )

val tiltakViFårFraKomet = setOf(
    "INDOPPFAG",
    "ARBFORB",
    "AVKLARAG",
    "VASV",
    "ARBRRHDAG",
    "DIGIOPPARB",
    "JOBBK",
    "GRUPPEAMO",
    "GRUFAGYRKE",
)
