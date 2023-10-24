package no.nav.tiltakspenger.tiltak.services

import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import no.nav.tiltakspenger.libs.arena.tiltak.ArenaTiltaksaktivitetResponsDTO
import no.nav.tiltakspenger.tiltak.clients.arena.ArenaClient
import no.nav.tiltakspenger.tiltak.clients.komet.DeltakerStatusDTO
import no.nav.tiltakspenger.tiltak.clients.komet.KometClient
import no.nav.tiltakspenger.tiltak.clients.tiltak.TiltakClient
import no.nav.tiltakspenger.tiltak.clients.valp.ValpClient
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
                .filter { it.status in kometStatusViVilHa }
                .filter { it.gjennomforing.type in tiltakViVilHaFraKomet }
                .map { deltakelse ->
                    securelog.info { deltakelse }
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
                            DeltakerStatusDTO.AVBRUTT -> DeltakerStatusResponseDTO.AVBRUTT
                            DeltakerStatusDTO.FULLFORT -> DeltakerStatusResponseDTO.FULLFORT
                            DeltakerStatusDTO.DELTAR -> DeltakerStatusResponseDTO.DELTAR
                            DeltakerStatusDTO.IKKE_AKTUELL -> DeltakerStatusResponseDTO.IKKE_AKTUELL
                            DeltakerStatusDTO.VENTER_PA_OPPSTART -> DeltakerStatusResponseDTO.VENTER_PA_OPPSTART
                            DeltakerStatusDTO.HAR_SLUTTET -> DeltakerStatusResponseDTO.HAR_SLUTTET

                            // Disse er vi ikke interresert i...
                            DeltakerStatusDTO.VURDERES -> DeltakerStatusResponseDTO.VURDERES
                            DeltakerStatusDTO.FEILREGISTRERT -> DeltakerStatusResponseDTO.FEILREGISTRERT
                            DeltakerStatusDTO.PABEGYNT_REGISTRERING -> DeltakerStatusResponseDTO.PABEGYNT_REGISTRERING
                            DeltakerStatusDTO.SOKT_INN -> DeltakerStatusResponseDTO.SOKT_INN
                            DeltakerStatusDTO.VENTELISTE -> DeltakerStatusResponseDTO.VENTELISTE
                        },
                    )
                }

            val arena = arenaClient.hentTiltakArena(fnr)
                .filter { it.deltakerStatusType in arenaStatusViVilHa }
                .filter { it.tiltakType.name in tiltakViVilHaFraArena }
                .map {
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
                            ArenaTiltaksaktivitetResponsDTO.DeltakerStatusType.DELAVB -> DeltakerStatusResponseDTO.AVBRUTT
                            ArenaTiltaksaktivitetResponsDTO.DeltakerStatusType.FULLF -> DeltakerStatusResponseDTO.FULLFORT
                            ArenaTiltaksaktivitetResponsDTO.DeltakerStatusType.GJENN -> DeltakerStatusResponseDTO.DELTAR
                            ArenaTiltaksaktivitetResponsDTO.DeltakerStatusType.GJENN_AVB -> DeltakerStatusResponseDTO.AVBRUTT
                            ArenaTiltaksaktivitetResponsDTO.DeltakerStatusType.IKKEM -> DeltakerStatusResponseDTO.IKKE_AKTUELL
                            ArenaTiltaksaktivitetResponsDTO.DeltakerStatusType.JATAKK -> DeltakerStatusResponseDTO.DELTAR
                            ArenaTiltaksaktivitetResponsDTO.DeltakerStatusType.TILBUD -> DeltakerStatusResponseDTO.VENTER_PA_OPPSTART
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
