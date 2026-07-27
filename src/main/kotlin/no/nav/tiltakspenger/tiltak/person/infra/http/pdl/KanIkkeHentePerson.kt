package no.nav.tiltakspenger.tiltak.person.infra.http.pdl

import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.HttpKlientMetadata

/**
 * Feil ved henting av identer fra PDL.
 * Variantene skiller mellom «kallet gikk ikke gjennom» og «vi fikk et svar, men ingen identer å bruke», fordi kalleren håndterer dem ulikt: det første er en reell feil, det andre gir fallback til innsendt fødselsnummer.
 */
sealed interface KanIkkeHentePerson {
    /** Selve HTTP-kallet feilet (transport, timeout, uventet status eller deserialisering). */
    data class KallFeilet(val httpKlientError: HttpKlientError) : KanIkkeHentePerson

    /**
     * PDL svarte 2xx, men med funksjonelle feil i errors-lista.
     * GraphQL svarer av design 200 OK på alle svar; feilmeldingene ligger i `errors`.
     */
    data class GraphQLFeil(
        val feilmeldinger: List<String>,
        val metadata: HttpKlientMetadata,
    ) : KanIkkeHentePerson

    /** PDL svarte uten feil, men uten identer — enten tom identliste eller `hentIdenter`/`data` som null. */
    data class FantIngenIdenter(
        val metadata: HttpKlientMetadata,
    ) : KanIkkeHentePerson
}
