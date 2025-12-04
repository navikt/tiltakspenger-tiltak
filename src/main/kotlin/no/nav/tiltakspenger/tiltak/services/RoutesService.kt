package no.nav.tiltakspenger.tiltak.services

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import no.nav.tiltakspenger.libs.logging.Sikkerlogg
import no.nav.tiltakspenger.libs.tiltak.TiltakResponsDTO.TiltakDTO
import no.nav.tiltakspenger.libs.tiltak.TiltakTilSaksbehandlingDTO
import no.nav.tiltakspenger.tiltak.clients.arena.ArenaClient
import no.nav.tiltakspenger.tiltak.clients.komet.KometClient
import no.nav.tiltakspenger.tiltak.clients.komet.toSaksbehandlingDTO
import no.nav.tiltakspenger.tiltak.clients.komet.toSøknadTiltak
import no.nav.tiltakspenger.tiltak.gjennomforing.db.Gjennomforing
import no.nav.tiltakspenger.tiltak.gjennomforing.db.GjennomforingRepo
import java.util.UUID

class RoutesService(
    private val kometClient: KometClient,
    private val arenaClient: ArenaClient,
    private val gjennomforingRepo: GjennomforingRepo,
) {
    private val log = KotlinLogging.logger {}

    fun hentTiltakForSaksbehandling(fnr: String, correlationId: String?): List<TiltakTilSaksbehandlingDTO> {
        val tiltakdeltakelser = runBlocking {
            val komet = kometClient.hentTiltakDeltagelser(fnr, correlationId)
                .filter { it.gjennomforing.type in tiltakViFårFraKomet }
                .map { deltakelse ->
                    deltakelse.toSaksbehandlingDTO(getGjennomforing(deltakelse.gjennomforing.id)?.deltidsprosent)
                }

            val arena = arenaClient.hentTiltakArena(fnr, correlationId)
                .filterNot { it.tiltakType.name in tiltakViFårFraKomet }
                .map {
                    Sikkerlogg.info { "Deltakelsene fra Arena vi mapper tilbake $it" }
                    it.toSaksbehandlingDTO()
                }
            arena + komet
        }

        return tiltakdeltakelser
    }

    fun hentTiltakForSøknad(fnr: String, correlationId: String?): List<TiltakDTO> {
        val tiltak = runBlocking {
            val komet = kometClient.hentTiltakDeltagelser(fnr, correlationId)
                .filter { it.gjennomforing.type in tiltakViFårFraKomet }
                .map { deltakelse ->
                    deltakelse.toSøknadTiltak(getGjennomforing(deltakelse.gjennomforing.id)?.deltidsprosent)
                }

            val arena = arenaClient.hentTiltakArena(fnr, correlationId)
                .filterNot { it.tiltakType.name in tiltakViFårFraKomet }
                .map {
                    Sikkerlogg.info { "Deltakelsene fra Arena vi mapper tilbake $it" }
                    it.toSøknadTiltak()
                }
            arena + komet
        }
        return tiltak
            .filter { it.deltakelseStatus.rettTilÅSøke }
            .filter { it.gjennomforing.arenaKode.rettPåTiltakspenger }
    }

    private fun getGjennomforing(gjennomforingId: String): Gjennomforing? {
        try {
            return gjennomforingRepo.hent(UUID.fromString(gjennomforingId))
        } catch (e: Exception) {
            log.warn { "Kunne ikke hente gjennomføring for id $gjennomforingId: ${e.message}" }
            return null
        }
    }
}
