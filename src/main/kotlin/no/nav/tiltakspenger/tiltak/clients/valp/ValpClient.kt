package no.nav.tiltakspenger.tiltak.clients.valp

interface ValpClient {
    fun hentTiltakGjennomføring(tiltakId: String)
}
