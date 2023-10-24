package no.nav.tiltakspenger.tiltak.clients.arena

import no.nav.tiltakspenger.libs.arena.tiltak.ArenaTiltaksaktivitetResponsDTO.TiltaksaktivitetDTO

interface ArenaClient {
    suspend fun hentTiltakArena(fnr: String): List<TiltaksaktivitetDTO>
}
