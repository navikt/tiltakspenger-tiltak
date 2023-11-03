package no.nav.tiltakspenger.tiltak.services

import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import no.nav.tiltakspenger.libs.arena.tiltak.ArenaTiltaksaktivitetResponsDTO
import no.nav.tiltakspenger.libs.arena.tiltak.ArenaTiltaksaktivitetResponsDTO.DeltakerStatusType
import no.nav.tiltakspenger.libs.tiltak.TiltakResponsDTO.DeltakerStatusResponseDTO
import no.nav.tiltakspenger.libs.tiltak.TiltakResponsDTO.GjennomforingResponseDTO
import no.nav.tiltakspenger.libs.tiltak.TiltakResponsDTO.TiltakDTO
import no.nav.tiltakspenger.libs.tiltak.TiltakResponsDTO.TiltakType
import no.nav.tiltakspenger.tiltak.clients.arena.ArenaClient
import no.nav.tiltakspenger.tiltak.clients.komet.DeltakerDTO
import no.nav.tiltakspenger.tiltak.clients.komet.DeltakerStatusDTO
import no.nav.tiltakspenger.tiltak.clients.komet.KometClient
import no.nav.tiltakspenger.tiltak.clients.tiltak.TiltakClient
import no.nav.tiltakspenger.tiltak.clients.valp.ValpClient
import no.nav.tiltakspenger.tiltak.clients.valp.ValpDTO
import java.time.LocalDate
import java.time.LocalDateTime

val securelog = KotlinLogging.logger("tjenestekall")

class RouteServiceImpl(
    private val kometClient: KometClient,
    private val valpClient: ValpClient,
    private val tiltakClient: TiltakClient,
    private val arenaClient: ArenaClient,
) : RoutesService {
    override fun hentAlleTiltak(fnr: String): List<TiltakDTO> {
        val tiltakdeltakelser = runBlocking {
            val kometOgValp = kometClient.hentTiltakDeltagelser(fnr)
                .also { securelog.info { "Hele svaret fra komet $it" } }
                .map { deltakelse ->
                    val gjennomføring = valpClient.hentTiltakGjennomføring(deltakelse.gjennomforing.id)
                        ?: throw IllegalStateException("Fant ikke gjennomføring i Valp")
                    securelog.info { "Deltakelsene vi mapper tilbake $deltakelse" }
                    mapKometTiltak(deltakelse, gjennomføring)
                }

            val arena = arenaClient.hentTiltakArena(fnr)
                .also { securelog.info { "Hele svaret fra arena $it" } }
                .filterNot { it.tiltakType.name in tiltakViFårFraKomet }
                .map {
                    securelog.info { "Deltakelsene fra Arena vi mapper tilbake $it" }
                    mapArenaTiltak(it)
                }

            arena + kometOgValp
        }

        return tiltakdeltakelser
    }

    override fun hentTiltakForSøknad(fnr: String): List<TiltakDTO> {
        return hentAlleTiltak(fnr)
            .filter { it.status.girRettTilÅASøke }
            .filter { it.gjennomforing.arenaKode.rettPåTiltakspenger }
    }
}

private fun mapKometTiltak(deltakelse: DeltakerDTO, gjennomføring: ValpDTO): TiltakDTO {
    return TiltakDTO(
        id = deltakelse.id,
        startDato = deltakelse.startDato,
        sluttDato = deltakelse.sluttDato,
        dagerPerUke = deltakelse.dagerPerUke,
        prosentStilling = deltakelse.prosentStilling,
        registrertDato = deltakelse.registrertDato,
        gjennomforing = GjennomforingResponseDTO(
            id = deltakelse.gjennomforing.id,
            arrangornavn = deltakelse.gjennomforing.arrangor.navn,
            typeNavn = gjennomføring.tiltakstype.navn,
            arenaKode = TiltakType.valueOf(gjennomføring.tiltakstype.arenaKode),
            startDato = gjennomføring.startDato,
            sluttDato = gjennomføring.sluttDato,
        ),
        status = when (deltakelse.status) {
            DeltakerStatusDTO.AVBRUTT -> DeltakerStatusResponseDTO.AVBRUTT
            DeltakerStatusDTO.FULLFORT -> DeltakerStatusResponseDTO.FULLFORT
            DeltakerStatusDTO.DELTAR -> DeltakerStatusResponseDTO.DELTAR
            DeltakerStatusDTO.IKKE_AKTUELL -> DeltakerStatusResponseDTO.IKKE_AKTUELL
            DeltakerStatusDTO.VENTER_PA_OPPSTART -> DeltakerStatusResponseDTO.VENTER_PA_OPPSTART
            DeltakerStatusDTO.HAR_SLUTTET -> DeltakerStatusResponseDTO.HAR_SLUTTET

            // Disse er ikke med i søknaden
            DeltakerStatusDTO.VURDERES -> DeltakerStatusResponseDTO.VURDERES
            DeltakerStatusDTO.FEILREGISTRERT -> DeltakerStatusResponseDTO.FEILREGISTRERT
            DeltakerStatusDTO.PABEGYNT_REGISTRERING -> DeltakerStatusResponseDTO.PABEGYNT_REGISTRERING
            DeltakerStatusDTO.SOKT_INN -> DeltakerStatusResponseDTO.SOKT_INN
            DeltakerStatusDTO.VENTELISTE -> DeltakerStatusResponseDTO.VENTELISTE
        },
    )
}

private fun mapArenaTiltak(tiltak: ArenaTiltaksaktivitetResponsDTO.TiltaksaktivitetDTO): TiltakDTO {
    return TiltakDTO(
        id = tiltak.aktivitetId,
        gjennomforing = GjennomforingResponseDTO(
            id = "",
            arrangornavn = tiltak.arrangoer ?: "Ukjent",
            typeNavn = tiltak.tiltakType.navn,
            arenaKode = TiltakType.valueOf(tiltak.tiltakType.name),
            startDato = null,
            sluttDato = null,
        ),
        startDato = earliest(tiltak.deltakelsePeriode?.fom, tiltak.deltakelsePeriode?.tom),
        sluttDato = latest(tiltak.deltakelsePeriode?.fom, tiltak.deltakelsePeriode?.tom),
        status = when (tiltak.deltakerStatusType) {
            DeltakerStatusType.DELAVB -> DeltakerStatusResponseDTO.AVBRUTT
            DeltakerStatusType.FULLF -> DeltakerStatusResponseDTO.FULLFORT
            DeltakerStatusType.GJENN -> DeltakerStatusResponseDTO.DELTAR
            DeltakerStatusType.GJENN_AVB -> DeltakerStatusResponseDTO.AVBRUTT
            DeltakerStatusType.IKKEM -> DeltakerStatusResponseDTO.AVBRUTT
            DeltakerStatusType.JATAKK -> DeltakerStatusResponseDTO.DELTAR
            DeltakerStatusType.TILBUD -> DeltakerStatusResponseDTO.VENTER_PA_OPPSTART

            // Disse er ikke med i søknaden
            DeltakerStatusType.AKTUELL -> DeltakerStatusResponseDTO.SOKT_INN
            DeltakerStatusType.AVSLAG -> DeltakerStatusResponseDTO.IKKE_AKTUELL
            DeltakerStatusType.GJENN_AVL -> DeltakerStatusResponseDTO.IKKE_AKTUELL
            DeltakerStatusType.IKKAKTUELL -> DeltakerStatusResponseDTO.IKKE_AKTUELL
            DeltakerStatusType.INFOMOETE -> DeltakerStatusResponseDTO.VENTELISTE
            DeltakerStatusType.NEITAKK -> DeltakerStatusResponseDTO.IKKE_AKTUELL
            DeltakerStatusType.VENTELISTE -> DeltakerStatusResponseDTO.VENTELISTE
        },
        dagerPerUke = tiltak.antallDagerPerUke,
        prosentStilling = tiltak.deltakelseProsent,
        registrertDato = LocalDateTime.from(tiltak.statusSistEndret?.atStartOfDay()) ?: LocalDateTime.now(),
    )
}

// Fordi Arena noen ganger bytter om på fom og tom, må vi bytte tilbake hvis det skjer...
private fun earliest(fom: LocalDate?, tom: LocalDate?) =
    when {
        fom != null && tom != null -> if (tom.isBefore(fom)) {
            securelog.warn { "fom er etter tom, så vi bytter om de to datoene på tiltaket" }
            tom
        } else {
            fom
        }

        else -> fom
    }

private fun latest(fom: LocalDate?, tom: LocalDate?) =
    when {
        fom != null && tom != null -> if (fom.isAfter(tom)) fom else tom
        else -> tom
    }

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
