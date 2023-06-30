package no.nav.tiltakspenger.tiltak.services

import kotlinx.coroutines.runBlocking
import no.nav.tiltakspenger.tiltak.clients.komet.KometClient
import no.nav.tiltakspenger.tiltak.clients.valp.ValpClient

class RouteServiceImpl(
    private val kometClient: KometClient,
    private val valpClient: ValpClient,
) : RoutesService {
    override fun hentTiltak(fnr: String) {
        val deltagelser = runBlocking {
            kometClient.hentTiltakDeltagelser(fnr)
        }
        valpClient.hentTiltakGjennomføring(deltagelser.deltakelser.first().gjennomforing.id.toString())
    }
}
