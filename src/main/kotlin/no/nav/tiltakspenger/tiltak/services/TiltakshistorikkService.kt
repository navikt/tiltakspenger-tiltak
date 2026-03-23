package no.nav.tiltakspenger.tiltak.services

import no.nav.tiltakspenger.journalposthendelser.journalpost.http.pdl.PdlClient
import no.nav.tiltakspenger.libs.logging.Sikkerlogg
import no.nav.tiltakspenger.libs.tiltak.TiltakshistorikkDTO
import no.nav.tiltakspenger.tiltak.Configuration
import no.nav.tiltakspenger.tiltak.clients.tiltakshistorikk.TiltakshistorikkClient
import no.nav.tiltakspenger.tiltak.clients.tiltakshistorikk.dto.KometDeltakerStatusDto
import no.nav.tiltakspenger.tiltak.clients.tiltakshistorikk.dto.TiltakshistorikkV1Dto

class TiltakshistorikkService(
    private val tiltakshistorikkClient: TiltakshistorikkClient,
    private val pdlClient: PdlClient,
) {
    suspend fun hentTiltakshistorikkForSaksbehandling(fnr: String): List<TiltakshistorikkDTO> {
        return hentTiltakshistorikk(fnr = fnr)
    }

    suspend fun hentTiltakshistorikkForSoknad(fnr: String): List<TiltakshistorikkDTO> {
        return hentTiltakshistorikk(fnr = fnr)
            .filter { it.deltakelseStatus.rettTilÅSøke }
            .filter { it.gjennomforing.arenaKode.rettPåTiltakspenger }
    }

    private suspend fun hentTiltakshistorikk(fnr: String): List<TiltakshistorikkDTO> {
        // Kommentar John: I første omgang fallbacker vi bare til innsendt fnr for å få en myk overgang. Lar denne feile ved null når vi har fjernet barnesykdommene. Legg på arrow plix.
        val nåværendePlussHistoriskeFnr = pdlClient.hentNåværendeOgHistoriskeFødselsnummer(fnr)
            ?.let { if (fnr in it) it else it + fnr }
            ?: listOf(fnr)
        val tiltakdeltakelser = tiltakshistorikkClient.hentTiltaksdeltakelser(nåværendePlussHistoriskeFnr)
            .filterNot { it is TiltakshistorikkV1Dto.TeamKometDeltakelse && it.status.type == KometDeltakerStatusDto.DeltakerStatusType.KLADD }
            .map { deltakelse ->
                Sikkerlogg.info { "Deltakelser fra tiltakshistorikk: $deltakelse" }
                when (deltakelse) {
                    is TiltakshistorikkV1Dto.TeamKometDeltakelse -> deltakelse.toTiltakshistorikkTilSaksbehandlingDTO()
                    is TiltakshistorikkV1Dto.ArenaDeltakelse -> deltakelse.toTiltakshistorikkTilSaksbehandlingDTO()
                    is TiltakshistorikkV1Dto.TeamTiltakAvtale -> deltakelse.toTiltakshistorikkTilSaksbehandlingDTO()
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
