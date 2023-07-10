package no.nav.tiltakspenger.tiltak.services

import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import no.nav.tiltakspenger.tiltak.clients.komet.DeltakerStatusDTO
import no.nav.tiltakspenger.tiltak.clients.komet.KometClient
import no.nav.tiltakspenger.tiltak.clients.valp.TiltaksgjennomforingOppstartstype
import no.nav.tiltakspenger.tiltak.clients.valp.Tiltaksgjennomforingsstatus
import no.nav.tiltakspenger.tiltak.clients.valp.ValpClient

val securelog = KotlinLogging.logger("tjenestekall")

// TODO Denne kalles fra en route som bare finnes i dev nå for test
class RouteServiceImpl(
    private val kometClient: KometClient,
    private val valpClient: ValpClient,
) : RoutesService {
    override fun hentTiltak(fnr: String): List<TiltakDeltakelseResponse> {
        val tiltakdeltakelser = runBlocking {
            kometClient.hentTiltakDeltagelser(fnr).map { deltakelse ->
                securelog.info { deltakelse }
//                val gjennomføring = valpClient.hentTiltakGjennomføring(deltakelse.gjennomforing.id.toString())
                TiltakDeltakelseResponse(
                    id = deltakelse.id,
                    startDato = deltakelse.startDato,
                    sluttDato = deltakelse.sluttDato,
                    dagerPerUke = deltakelse.dagerPerUke,
                    prosentStilling = deltakelse.prosentStilling,
                    registrertDato = deltakelse.registrertDato,
                    gjennomforing = GjennomforingResponseDTO(
                        id = deltakelse.gjennomforing.id,
                        navn = deltakelse.gjennomforing.navn,
                        type = deltakelse.gjennomforing.type,
                        arrangor = ArrangorResponseDTO(
                            virksomhetsnummer = deltakelse.gjennomforing.arrangor.virksomhetsnummer,
                            navn = deltakelse.gjennomforing.arrangor.navn,
                        ),
                        valp = valpClient.hentTiltakGjennomføring(deltakelse.gjennomforing.id.toString())?.let {
                            securelog.info { it }
                            ValpResponse(
                                id = it.id,
                                tiltakstype = TiltakstypeResponse(
                                    id = it.tiltakstype.id,
                                    navn = it.tiltakstype.navn,
                                    arenaKode = it.tiltakstype.arenaKode,
                                ),
                                navn = it.navn,
                                startDato = it.startDato,
                                sluttDato = it.sluttDato,
                                status = when (it.status) {
                                    Tiltaksgjennomforingsstatus.GJENNOMFORES -> TiltaksgjennomforingsstatusResponse.GJENNOMFORES
                                    Tiltaksgjennomforingsstatus.AVBRUTT -> TiltaksgjennomforingsstatusResponse.AVBRUTT
                                    Tiltaksgjennomforingsstatus.AVLYST -> TiltaksgjennomforingsstatusResponse.AVLYST
                                    Tiltaksgjennomforingsstatus.AVSLUTTET -> TiltaksgjennomforingsstatusResponse.AVSLUTTET
                                    Tiltaksgjennomforingsstatus.APENT_FOR_INNSOK -> TiltaksgjennomforingsstatusResponse.APENT_FOR_INNSOK
                                },
                                virksomhetsnummer = it.virksomhetsnummer,
                                oppstart = when (it.oppstart) {
                                    TiltaksgjennomforingOppstartstype.LOPENDE -> TiltaksgjennomforingOppstartstypeResponse.LOPENDE
                                    TiltaksgjennomforingOppstartstype.FELLES -> TiltaksgjennomforingOppstartstypeResponse.FELLES
                                },
                            )
                        },
                    ),
                    status = when (deltakelse.status) {
                        DeltakerStatusDTO.VENTER_PA_OPPSTART -> DeltakerStatusResponseDTO.VENTER_PA_OPPSTART
                        DeltakerStatusDTO.DELTAR -> DeltakerStatusResponseDTO.DELTAR
                        DeltakerStatusDTO.HAR_SLUTTET -> DeltakerStatusResponseDTO.HAR_SLUTTET
                        DeltakerStatusDTO.IKKE_AKTUELL -> DeltakerStatusResponseDTO.IKKE_AKTUELL
                        DeltakerStatusDTO.VURDERES -> DeltakerStatusResponseDTO.VURDERES
                        DeltakerStatusDTO.AVBRUTT -> DeltakerStatusResponseDTO.AVBRUTT
                    },
                )
            }
        }

        return tiltakdeltakelser
    }
}
