package no.nav.tiltakspenger.tiltak.services

import kotlinx.coroutines.runBlocking
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.logging.Sikkerlogg
import no.nav.tiltakspenger.tiltak.clients.arena.ArenaClient
import no.nav.tiltakspenger.tiltak.clients.tiltakshistorikk.TiltakshistorikkClient
import no.nav.tiltakspenger.tiltak.clients.tiltakshistorikk.dto.KometDeltakerStatusDto
import no.nav.tiltakspenger.tiltak.clients.tiltakshistorikk.dto.TiltakshistorikkV1Dto
import no.nav.tiltakspenger.tiltak.routes.TiltakshistorikkTilSaksbehandlingDTO

class TiltakshistorikkService(
    private val tiltakshistorikkClient: TiltakshistorikkClient,
    private val arenaClient: ArenaClient,
) {
    fun hentTiltakshistorikkForSaksbehandling(fnr: String, correlationId: String?): List<TiltakshistorikkTilSaksbehandlingDTO> {
        val tiltakdeltakelser = runBlocking {
            val tiltakshistorikk = tiltakshistorikkClient.hentTiltaksdeltakelser(fnr)
                .filterNot { it.opphav == TiltakshistorikkV1Dto.Opphav.TEAM_TILTAK }
                .filterNot { it is TiltakshistorikkV1Dto.TeamKometDeltakelse && it.status.type == KometDeltakerStatusDto.DeltakerStatusType.KLADD }
                .map { deltakelse ->
                    Sikkerlogg.info { "Deltakelser fra tiltakshistorikk: ${objectMapper.writeValueAsString(deltakelse)}" }
                    when (deltakelse) {
                        is TiltakshistorikkV1Dto.TeamKometDeltakelse -> deltakelse.toTiltakshistorikkTilSaksbehandlingDTO()
                        is TiltakshistorikkV1Dto.ArenaDeltakelse -> deltakelse.toTiltakshistorikkTilSaksbehandlingDTO()
                        is TiltakshistorikkV1Dto.TeamTiltakAvtale -> throw IllegalArgumentException("Skal ikke hente team tiltak sine deltakelser fra tiltakshistorikk")
                    }
                }

            val arena = arenaClient.hentTiltakArena(fnr, correlationId)
                .filter { it.tiltakType.name in tiltakFraTeamTiltak }
                .map {
                    Sikkerlogg.info { "Deltakelser fra Arena: $it" }
                    it.toTiltakshistorikkTilSaksbehandlingDTO()
                }
            arena + tiltakshistorikk
        }

        return tiltakdeltakelser.filter { tiltak ->
            // Filtrerer bort tiltak for til og med dato er etter fra og med
            val fom = tiltak.deltakelseFom ?: return@filter true
            val tom = tiltak.deltakelseTom ?: return@filter true
            fom <= tom
        }
    }
}

val tiltakFraTeamTiltak = setOf(
    "ARBTREN",
    "MIDLONTIL",
    "VARLONTIL",
    "MENTOR",
    "INKLUTILS",
    "VATIAROR",
    "TILSJOBB",
)
