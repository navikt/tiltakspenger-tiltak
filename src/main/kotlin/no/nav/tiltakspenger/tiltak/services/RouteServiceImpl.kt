package no.nav.tiltakspenger.tiltak.services

import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import no.nav.tiltakspenger.tiltak.clients.komet.KometClient
import no.nav.tiltakspenger.tiltak.clients.valp.ValpClient

val securelog = KotlinLogging.logger("tjenestekall")

class RouteServiceImpl(
    private val kometClient: KometClient,
    private val valpClient: ValpClient,
) : RoutesService {
    override fun hentTiltak(fnr: String) {
        val deltakelser = runBlocking {
            kometClient.hentTiltakDeltagelser(fnr)
        }
        securelog.info { deltakelser }
        val valpResponse = runBlocking {
            deltakelser.map {
                valpClient.hentTiltakGjennomføring(it.gjennomforing.id.toString())
            }
        }
        securelog.info { valpResponse }
    }
}
