package no.nav.tiltakspenger.tiltak.services

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.httpklient.loggFeil
import no.nav.tiltakspenger.libs.logging.Sikkerlogg
import no.nav.tiltakspenger.libs.tiltak.TiltakshistorikkDTO
import no.nav.tiltakspenger.tiltak.clients.tiltakshistorikk.TiltakshistorikkClient
import no.nav.tiltakspenger.tiltak.clients.tiltakshistorikk.dto.KometDeltakerStatusDto
import no.nav.tiltakspenger.tiltak.clients.tiltakshistorikk.dto.TiltakshistorikkV1Dto
import no.nav.tiltakspenger.tiltak.person.infra.http.pdl.KanIkkeHentePerson
import no.nav.tiltakspenger.tiltak.person.infra.http.pdl.PdlClient
import java.time.Clock

/**
 * Klientene er stille og returnerer `Either`; denne servicen er laget som har domenekonteksten og logger derfor hver feilsituasjon nøyaktig én gang.
 */
class TiltakshistorikkService(
    private val tiltakshistorikkClient: TiltakshistorikkClient,
    private val pdlClient: PdlClient,
    private val clock: Clock,
    private val sikkerlogg: Sikkerlogg = Sikkerlogg,
) {
    private val logger = KotlinLogging.logger {}

    suspend fun hentTiltakshistorikkForSaksbehandling(fnr: String): Either<KunneIkkeHenteTiltakshistorikk, List<TiltakshistorikkDTO>> {
        return hentTiltakshistorikk(fnr = fnr)
    }

    suspend fun hentTiltakshistorikkForSoknad(fnr: String): Either<KunneIkkeHenteTiltakshistorikk, List<TiltakshistorikkDTO>> {
        return hentTiltakshistorikk(fnr = fnr).map { tiltakshistorikk ->
            tiltakshistorikk
                .filter { it.deltakelseStatus.rettTilÅSøke }
                .filter { it.gjennomforing.arenaKode.rettPåTiltakspenger }
        }
    }

    private suspend fun hentTiltakshistorikk(fnr: String): Either<KunneIkkeHenteTiltakshistorikk, List<TiltakshistorikkDTO>> {
        return hentNåværendeOgHistoriskeFnr(fnr).flatMap { identer ->
            tiltakshistorikkClient.hentTiltaksdeltakelser(identer)
                .mapLeft { feil ->
                    feil.loggFeil(
                        logger = logger,
                        operasjon = "henting av tiltaksdeltakelser fra tiltakshistorikk",
                        kontekst = "Antall identer i oppslaget: ${identer.size}",
                        sikkerlogg = sikkerlogg,
                    )
                    KunneIkkeHenteTiltakshistorikk
                }.map { deltakelser -> deltakelser.tilTiltakshistorikkDTO() }
        }
    }

    /**
     * Kommentar John: I første omgang fallbacker vi bare til innsendt fnr for å få en myk overgang.
     * Lar denne feile ved null når vi har fjernet barnesykdommene.
     *
     * Fallbacken gjelder svarene der PDL ikke ga oss identer å bruke — den gamle klienten returnerte `null` for nøyaktig de tilfellene.
     * Feilet selve kallet, feiler også oppslaget vårt, slik det gjorde da klienten kastet.
     */
    private suspend fun hentNåværendeOgHistoriskeFnr(fnr: String): Either<KunneIkkeHenteTiltakshistorikk, List<String>> {
        return pdlClient.hentNåværendeOgHistoriskeFødselsnummer(fnr).fold(
            ifLeft = { feil ->
                feil.logg()
                when (feil) {
                    is KanIkkeHentePerson.KallFeilet -> KunneIkkeHenteTiltakshistorikk.left()
                    is KanIkkeHentePerson.GraphQLFeil, is KanIkkeHentePerson.FantIngenIdenter -> listOf(fnr).right()
                }
            },
            ifRight = { identer -> (if (fnr in identer) identer else identer + fnr).right() },
        )
    }

    /** Én logghendelse per feilsituasjon: vanlig logg uten personopplysninger, sikkerlogg med rå request/respons. */
    private fun KanIkkeHentePerson.logg() {
        when (this) {
            is KanIkkeHentePerson.KallFeilet -> httpKlientError.loggFeil(
                logger = logger,
                operasjon = "henting av identer fra PDL",
                kontekst = "Faller ikke tilbake til innsendt fnr; oppslaget feiler",
                sikkerlogg = sikkerlogg,
            )

            is KanIkkeHentePerson.GraphQLFeil -> {
                logger.error { "PDL svarte med GraphQL-feil ved henting av identer. Faller tilbake til innsendt fnr. ${sikkerlogg.seSikkerlogg}" }
                sikkerlogg.error { "PDL svarte med GraphQL-feil ved henting av identer: $feilmeldinger. response: ${metadata.rawResponseString}" }
            }

            is KanIkkeHentePerson.FantIngenIdenter -> {
                logger.error { "Fant ingen identer i PDL. Faller tilbake til innsendt fnr. ${sikkerlogg.seSikkerlogg}" }
                sikkerlogg.error { "Fant ingen identer i PDL. response: ${metadata.rawResponseString}" }
            }
        }
    }

    private fun List<TiltakshistorikkV1Dto>.tilTiltakshistorikkDTO(): List<TiltakshistorikkDTO> {
        val tiltakdeltakelser = this
            .filterNot { it is TiltakshistorikkV1Dto.TeamKometDeltakelse && it.status.type == KometDeltakerStatusDto.DeltakerStatusType.KLADD }
            .map { deltakelse ->
                sikkerlogg.info { "Deltakelser fra tiltakshistorikk: $deltakelse" }
                when (deltakelse) {
                    is TiltakshistorikkV1Dto.TeamKometDeltakelse -> deltakelse.toTiltakshistorikkTilSaksbehandlingDTO()
                    is TiltakshistorikkV1Dto.ArenaDeltakelse -> deltakelse.toTiltakshistorikkTilSaksbehandlingDTO(clock)
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
