package no.nav.tiltakspenger.tiltak.services

import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import no.nav.tiltakspenger.libs.arena.tiltak.ArenaTiltaksaktivitetResponsDTO
import no.nav.tiltakspenger.libs.arena.tiltak.ArenaTiltaksaktivitetResponsDTO.DeltakerStatusType
import no.nav.tiltakspenger.libs.tiltak.TiltakResponsDTO
import no.nav.tiltakspenger.libs.tiltak.TiltakResponsDTO.DeltakerStatusDTO
import no.nav.tiltakspenger.libs.tiltak.TiltakResponsDTO.TiltakDTO
import no.nav.tiltakspenger.libs.tiltak.TiltakResponsDTO.TiltakType
import no.nav.tiltakspenger.tiltak.clients.arena.ArenaClient
import no.nav.tiltakspenger.tiltak.clients.komet.DeltakerDTO
import no.nav.tiltakspenger.tiltak.clients.komet.DeltakerStatusDTO.AVBRUTT
import no.nav.tiltakspenger.tiltak.clients.komet.DeltakerStatusDTO.DELTAR
import no.nav.tiltakspenger.tiltak.clients.komet.DeltakerStatusDTO.FEILREGISTRERT
import no.nav.tiltakspenger.tiltak.clients.komet.DeltakerStatusDTO.FULLFORT
import no.nav.tiltakspenger.tiltak.clients.komet.DeltakerStatusDTO.HAR_SLUTTET
import no.nav.tiltakspenger.tiltak.clients.komet.DeltakerStatusDTO.IKKE_AKTUELL
import no.nav.tiltakspenger.tiltak.clients.komet.DeltakerStatusDTO.PABEGYNT_REGISTRERING
import no.nav.tiltakspenger.tiltak.clients.komet.DeltakerStatusDTO.SOKT_INN
import no.nav.tiltakspenger.tiltak.clients.komet.DeltakerStatusDTO.VENTELISTE
import no.nav.tiltakspenger.tiltak.clients.komet.DeltakerStatusDTO.VENTER_PA_OPPSTART
import no.nav.tiltakspenger.tiltak.clients.komet.DeltakerStatusDTO.VURDERES
import no.nav.tiltakspenger.tiltak.clients.komet.KometClient
import no.nav.tiltakspenger.tiltak.clients.tiltak.TiltakClient
import no.nav.tiltakspenger.tiltak.clients.valp.ValpClient
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
                    securelog.info { "Deltakelsene vi mapper tilbake $deltakelse" }
                    mapKometTiltak(deltakelse)
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
            .filter { it.deltakelseStatus.rettTilÅSøke }
            .filter { it.gjennomforing.arenaKode.rettPåTiltakspenger }
    }
}

private fun mapKometTiltak(deltakelse: DeltakerDTO): TiltakDTO {
    return TiltakDTO(
        id = deltakelse.id,
        deltakelseFom = deltakelse.startDato,
        deltakelseTom = deltakelse.sluttDato,
        deltakelseDagerUke = deltakelse.dagerPerUke,
        deltakelseProsent = deltakelse.prosentStilling,
        registrertDato = deltakelse.registrertDato,
        gjennomforing = TiltakResponsDTO.GjennomføringDTO(
            id = deltakelse.gjennomforing.id,
            arrangørnavn = deltakelse.gjennomforing.arrangor.navn,
            typeNavn = deltakelse.gjennomforing.tiltakstypeNavn,
            arenaKode = TiltakType.valueOf(deltakelse.gjennomforing.type),
            fom = null,
            tom = null,
        ),
        kilde = "Komet",
        deltakelseStatus = when (deltakelse.status) {
            AVBRUTT -> DeltakerStatusDTO.AVBRUTT
            FULLFORT -> DeltakerStatusDTO.FULLFORT
            DELTAR -> DeltakerStatusDTO.DELTAR
            IKKE_AKTUELL -> DeltakerStatusDTO.IKKE_AKTUELL
            VENTER_PA_OPPSTART -> DeltakerStatusDTO.VENTER_PA_OPPSTART
            HAR_SLUTTET -> DeltakerStatusDTO.HAR_SLUTTET

            // Disse er ikke med i søknaden
            VURDERES -> DeltakerStatusDTO.VURDERES
            FEILREGISTRERT -> DeltakerStatusDTO.FEILREGISTRERT
            PABEGYNT_REGISTRERING -> DeltakerStatusDTO.PABEGYNT_REGISTRERING
            SOKT_INN -> DeltakerStatusDTO.SOKT_INN
            VENTELISTE -> DeltakerStatusDTO.VENTELISTE
        },
    )
}

private fun mapArenaTiltak(tiltak: ArenaTiltaksaktivitetResponsDTO.TiltaksaktivitetDTO): TiltakDTO {
    val fom = earliest(tiltak.deltakelsePeriode?.fom, tiltak.deltakelsePeriode?.tom) ?: LocalDate.MAX
    val startDatoErFremITid = (fom.isAfter(LocalDate.now()))
    return TiltakDTO(
        id = tiltak.aktivitetId,
        gjennomforing = TiltakResponsDTO.GjennomføringDTO(
            id = "",
            arrangørnavn = tiltak.arrangoer ?: "Ukjent",
            typeNavn = tiltak.tiltakType.navn,
            arenaKode = TiltakType.valueOf(tiltak.tiltakType.name),
            fom = null,
            tom = null,
        ),
        deltakelseFom = earliest(tiltak.deltakelsePeriode?.fom, tiltak.deltakelsePeriode?.tom),
        deltakelseTom = latest(tiltak.deltakelsePeriode?.fom, tiltak.deltakelsePeriode?.tom),
        deltakelseStatus = when (tiltak.deltakerStatusType) {
            DeltakerStatusType.DELAVB -> DeltakerStatusDTO.AVBRUTT
            DeltakerStatusType.FULLF -> DeltakerStatusDTO.FULLFORT
            DeltakerStatusType.GJENN -> if (startDatoErFremITid) DeltakerStatusDTO.VENTER_PA_OPPSTART else DeltakerStatusDTO.DELTAR
            DeltakerStatusType.GJENN_AVB -> DeltakerStatusDTO.AVBRUTT
            DeltakerStatusType.IKKEM -> DeltakerStatusDTO.AVBRUTT
            DeltakerStatusType.JATAKK -> DeltakerStatusDTO.DELTAR
            DeltakerStatusType.TILBUD -> if (startDatoErFremITid) DeltakerStatusDTO.VENTER_PA_OPPSTART else DeltakerStatusDTO.DELTAR

            // Disse er ikke med i søknaden
            DeltakerStatusType.AKTUELL -> DeltakerStatusDTO.SOKT_INN
            DeltakerStatusType.AVSLAG -> DeltakerStatusDTO.IKKE_AKTUELL
            DeltakerStatusType.GJENN_AVL -> DeltakerStatusDTO.IKKE_AKTUELL
            DeltakerStatusType.IKKAKTUELL -> DeltakerStatusDTO.IKKE_AKTUELL
            DeltakerStatusType.INFOMOETE -> DeltakerStatusDTO.VENTELISTE
            DeltakerStatusType.NEITAKK -> DeltakerStatusDTO.IKKE_AKTUELL
            DeltakerStatusType.VENTELISTE -> DeltakerStatusDTO.VENTELISTE
        },
        deltakelseDagerUke = tiltak.antallDagerPerUke,
        deltakelseProsent = tiltak.deltakelseProsent,
        kilde = "Arena",
        registrertDato = tiltak.statusSistEndret?.let { LocalDateTime.from(it.atStartOfDay()) } ?: LocalDateTime.now(),
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
