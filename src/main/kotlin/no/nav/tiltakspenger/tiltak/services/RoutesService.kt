package no.nav.tiltakspenger.tiltak.services

import kotlinx.coroutines.runBlocking
import no.nav.tiltakspenger.libs.logging.Sikkerlogg
import no.nav.tiltakspenger.libs.tiltak.TiltakResponsDTO.TiltakDTO
import no.nav.tiltakspenger.libs.tiltak.TiltakTilSaksbehandlingDTO
import no.nav.tiltakspenger.tiltak.clients.arena.ArenaClient
import no.nav.tiltakspenger.tiltak.clients.komet.KometClient
import no.nav.tiltakspenger.tiltak.clients.komet.toSaksbehandlingDTO
import no.nav.tiltakspenger.tiltak.clients.komet.toSøknadTiltak
import no.nav.tiltakspenger.tiltak.gjennomforing.db.GjennomforingRepo
import java.util.UUID

class RoutesService(
    private val kometClient: KometClient,
    private val arenaClient: ArenaClient,
    private val gjennomforingRepo: GjennomforingRepo,
) {
    fun hentTiltakForSaksbehandling(fnr: String, correlationId: String?): List<TiltakTilSaksbehandlingDTO> {
        val tiltakdeltakelser = runBlocking {
            val komet = kometClient.hentTiltakDeltagelser(fnr, correlationId)
                .map { deltakelse ->
                    val gjennomforing = gjennomforingRepo.hent(UUID.fromString(deltakelse.gjennomforing.id))
                    deltakelse.toSaksbehandlingDTO(gjennomforing?.deltidsprosent)
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
                .map { deltakelse ->
                    val gjennomforing = gjennomforingRepo.hent(UUID.fromString(deltakelse.gjennomforing.id))
                    deltakelse.toSøknadTiltak(gjennomforing?.deltidsprosent)
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
}
