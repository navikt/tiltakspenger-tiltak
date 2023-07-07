package no.nav.tiltakspenger.tiltak.clients.valp

interface ValpClient {
    suspend fun hentTiltakGjennomføring(tiltakId: String): ValpDTO?
}
