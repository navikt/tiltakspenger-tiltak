package no.nav.tiltakspenger.tiltak.clients.komet

interface KometClient {
    suspend fun hentTiltakDeltagelser(fnr: String): List<DeltakerDTO>
}
