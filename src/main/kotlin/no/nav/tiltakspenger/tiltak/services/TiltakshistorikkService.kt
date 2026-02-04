package no.nav.tiltakspenger.tiltak.services

import kotlinx.coroutines.runBlocking
import no.nav.tiltakspenger.libs.logging.Sikkerlogg
import no.nav.tiltakspenger.libs.tiltak.TiltakshistorikkDTO
import no.nav.tiltakspenger.tiltak.clients.tiltakshistorikk.TiltakshistorikkClient
import no.nav.tiltakspenger.tiltak.clients.tiltakshistorikk.dto.KometDeltakerStatusDto
import no.nav.tiltakspenger.tiltak.clients.tiltakshistorikk.dto.TiltakshistorikkV1Dto

class TiltakshistorikkService(
    private val tiltakshistorikkClient: TiltakshistorikkClient,
) {
    fun hentTiltakshistorikkForSaksbehandling(fnr: String): List<TiltakshistorikkDTO> {
        return hentTiltakshistorikk(fnr = fnr)
    }

    fun hentTiltakshistorikkForSoknad(fnr: String): List<TiltakshistorikkDTO> {
        return hentTiltakshistorikk(fnr = fnr)
            .filter { it.deltakelseStatus.rettTilÅSøke }
            .filter { it.gjennomforing.arenaKode.rettPåTiltakspenger }
    }

    private fun hentTiltakshistorikk(fnr: String): List<TiltakshistorikkDTO> {
        val tiltakdeltakelser = runBlocking {
            tiltakshistorikkClient.hentTiltaksdeltakelser(fnr)
                .filterNot { it is TiltakshistorikkV1Dto.TeamKometDeltakelse && it.status.type == KometDeltakerStatusDto.DeltakerStatusType.KLADD }
                .map { deltakelse ->
                    Sikkerlogg.info { "Deltakelser fra tiltakshistorikk: $deltakelse" }
                    when (deltakelse) {
                        is TiltakshistorikkV1Dto.TeamKometDeltakelse -> deltakelse.toTiltakshistorikkTilSaksbehandlingDTO()
                        is TiltakshistorikkV1Dto.ArenaDeltakelse -> deltakelse.toTiltakshistorikkTilSaksbehandlingDTO()
                        is TiltakshistorikkV1Dto.TeamTiltakAvtale -> deltakelse.toTiltakshistorikkTilSaksbehandlingDTO()
                    }
                }
        }
        return tiltakdeltakelser.filter { tiltak ->
            // Filtrerer bort tiltak for til og med dato er etter fra og med
            val fom = tiltak.deltakelseFom ?: return@filter true
            val tom = tiltak.deltakelseTom ?: return@filter true
            fom <= tom
        }
    }
}
