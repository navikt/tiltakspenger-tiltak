package no.nav.tiltakspenger.tiltak.services

import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import no.nav.tiltakspenger.libs.arena.tiltak.ArenaTiltaksaktivitetResponsDTO.DeltakerStatusType
import no.nav.tiltakspenger.tiltak.clients.arena.ArenaClient
import no.nav.tiltakspenger.tiltak.clients.komet.DeltakerStatusDTO
import no.nav.tiltakspenger.tiltak.clients.komet.KometClient
import no.nav.tiltakspenger.tiltak.clients.tiltak.TiltakClient
import no.nav.tiltakspenger.tiltak.clients.valp.ValpClient
import no.nav.tiltakspenger.tiltak.services.DeltakerStatusResponseDTO.AVBRUTT
import no.nav.tiltakspenger.tiltak.services.DeltakerStatusResponseDTO.DELTAR
import no.nav.tiltakspenger.tiltak.services.DeltakerStatusResponseDTO.FEILREGISTRERT
import no.nav.tiltakspenger.tiltak.services.DeltakerStatusResponseDTO.FULLFORT
import no.nav.tiltakspenger.tiltak.services.DeltakerStatusResponseDTO.HAR_SLUTTET
import no.nav.tiltakspenger.tiltak.services.DeltakerStatusResponseDTO.IKKE_AKTUELL
import no.nav.tiltakspenger.tiltak.services.DeltakerStatusResponseDTO.PABEGYNT_REGISTRERING
import no.nav.tiltakspenger.tiltak.services.DeltakerStatusResponseDTO.SOKT_INN
import no.nav.tiltakspenger.tiltak.services.DeltakerStatusResponseDTO.VENTELISTE
import no.nav.tiltakspenger.tiltak.services.DeltakerStatusResponseDTO.VENTER_PA_OPPSTART
import no.nav.tiltakspenger.tiltak.services.DeltakerStatusResponseDTO.VURDERES
import java.time.LocalDateTime

val securelog = KotlinLogging.logger("tjenestekall")

// TODO Denne kalles fra en route som bare finnes i dev nå for test
class RouteServiceImpl(
    private val kometClient: KometClient,
    private val valpClient: ValpClient,
    private val tiltakClient: TiltakClient,
    private val arenaClient: ArenaClient,
) : RoutesService {
    override fun hentTiltak(fnr: String): List<TiltakDeltakelseResponse> {
        val tiltakdeltakelser = runBlocking {
            val kometOgValp = kometClient.hentTiltakDeltagelser(fnr)
                .also { securelog.info { "Hele svaret fra komet $it" } }
                .filter { it.status in kometStatusViVilHa }
                .filter { it.gjennomforing.type in tiltakViVilHaFraKomet }
                .map { deltakelse ->
                    securelog.info { "Deltakelsene vi mapper tilbake $deltakelse" }
                    val gjennomføring = valpClient.hentTiltakGjennomføring(deltakelse.gjennomforing.id)
                        ?: throw IllegalStateException("Fant ikke gjennomføring i Valp")
                    TiltakDeltakelseResponse(
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
                            arenaKode = gjennomføring.tiltakstype.arenaKode,
                            startDato = gjennomføring.startDato,
                            sluttDato = gjennomføring.sluttDato,
                        ),
                        status = when (deltakelse.status) {
                            DeltakerStatusDTO.AVBRUTT -> AVBRUTT
                            DeltakerStatusDTO.FULLFORT -> FULLFORT
                            DeltakerStatusDTO.DELTAR -> DELTAR
                            DeltakerStatusDTO.IKKE_AKTUELL -> IKKE_AKTUELL
                            DeltakerStatusDTO.VENTER_PA_OPPSTART -> VENTER_PA_OPPSTART
                            DeltakerStatusDTO.HAR_SLUTTET -> HAR_SLUTTET

                            // Disse er vi ikke interresert i, de blir filtrer bort i tiltakViVilHaFraKomet...
                            DeltakerStatusDTO.VURDERES -> VURDERES
                            DeltakerStatusDTO.FEILREGISTRERT -> FEILREGISTRERT
                            DeltakerStatusDTO.PABEGYNT_REGISTRERING -> PABEGYNT_REGISTRERING
                            DeltakerStatusDTO.SOKT_INN -> SOKT_INN
                            DeltakerStatusDTO.VENTELISTE -> VENTELISTE
                        },
                    )
                }

            val arena = arenaClient.hentTiltakArena(fnr)
                .also { securelog.info { "Hele svaret fra arena $it" } }
                .filter { it.deltakerStatusType in arenaStatusViVilHa }
                .filter { it.tiltakType.name in tiltakViVilHaFraArena }
                .map {
                    securelog.info { "Deltakelsene fra Arena vi mapper tilbake $it" }
                    TiltakDeltakelseResponse(
                        id = it.aktivitetId,
                        gjennomforing = GjennomforingResponseDTO(
                            id = "",
                            arrangornavn = it.arrangoer ?: "Ukjent",
                            typeNavn = it.tiltakType.navn,
                            arenaKode = it.tiltakType.name,
                            startDato = null,
                            sluttDato = null,
                        ),
                        startDato = it.deltakelsePeriode?.fom,
                        sluttDato = it.deltakelsePeriode?.tom,
                        status = when (val status = it.deltakerStatusType) {
                            DeltakerStatusType.DELAVB -> AVBRUTT
                            DeltakerStatusType.FULLF -> FULLFORT
                            DeltakerStatusType.GJENN -> DELTAR
                            DeltakerStatusType.GJENN_AVB -> AVBRUTT
                            DeltakerStatusType.IKKEM -> IKKE_AKTUELL
                            DeltakerStatusType.JATAKK -> DELTAR
                            DeltakerStatusType.TILBUD -> VENTER_PA_OPPSTART
                            else -> throw IllegalStateException("Fikk en staus fra Arena vi ikke vil ha $status")
                        },
                        dagerPerUke = it.antallDagerPerUke,
                        prosentStilling = it.deltakelseProsent,
                        registrertDato = LocalDateTime.from(it.statusSistEndret),
                    )
                }

            arena + kometOgValp
        }

        return tiltakdeltakelser
    }
}
