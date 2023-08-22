package no.nav.tiltakspenger.tiltak.clients.tiltak

interface TiltakClient {
    suspend fun hentTiltak(fnr: String): TiltakDTO?
}
