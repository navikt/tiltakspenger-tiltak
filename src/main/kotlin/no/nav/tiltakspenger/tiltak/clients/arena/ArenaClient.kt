package no.nav.tiltakspenger.tiltak.clients.arena

interface ArenaClient {
    suspend fun hentTiltakArena(fnr: String): ArenaDTO?
}
