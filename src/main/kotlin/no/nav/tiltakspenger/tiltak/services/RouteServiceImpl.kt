package no.nav.tiltakspenger.tiltak.services

import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import no.nav.tiltakspenger.tiltak.clients.komet.KometClient

val securelog = KotlinLogging.logger("tjenestekall")

class RouteServiceImpl(
    private val kometClient: KometClient,
//    private val valpClient: ValpClient,
) : RoutesService {
    override fun hentTiltak(fnr: String) {
        val deltakelser = runBlocking {
            kometClient.hentTiltakDeltagelser(fnr)
        }
        securelog.info { deltakelser }
//        valpClient.hentTiltakGjennomføring(deltagelser.deltakelser.first().gjennomforing.id.toString())
    }
}
