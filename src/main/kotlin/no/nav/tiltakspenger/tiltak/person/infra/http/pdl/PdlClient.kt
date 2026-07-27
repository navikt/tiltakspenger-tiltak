package no.nav.tiltakspenger.tiltak.person.infra.http.pdl

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.httpklient.HttpKlientMetadata
import no.nav.tiltakspenger.libs.httpklient.infra.HttpKlient
import no.nav.tiltakspenger.libs.httpklient.infra.HttpKlientConfig
import no.nav.tiltakspenger.libs.httpklient.infra.kall.AuthTokenProvider
import no.nav.tiltakspenger.libs.httpklient.infra.kall.KlientAuth
import no.nav.tiltakspenger.libs.httpklient.infra.kall.NavHeadere
import no.nav.tiltakspenger.libs.httpklient.infra.kall.Statusregel
import no.nav.tiltakspenger.libs.httpklient.infra.retry.Retry
import no.nav.tiltakspenger.libs.httpklient.infra.transport.HttpTransport
import no.nav.tiltakspenger.libs.httpklient.infra.transport.JavaHttpTransport
import no.nav.tiltakspenger.tiltak.infra.http.GraphQLResponse
import java.net.URI
import java.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Klient for å hente identer fra PDL (persondataløsningen).
 *
 * Kildekode: https://github.com/navikt/pdl
 * Dokumentasjon: https://pdl-docs.ansatt.nav.no/
 * API-spec: https://github.com/navikt/pdl/blob/15bdc571f0357f97f524dc496fb16217ff4aa94d/apps/api/src/main/resources/schemas/pdl.graphqls#L17 og https://pdl-playground.dev.intern.nav.no/ og https://pdl-pip-api.intern.dev.nav.no/swagger-ui/index.html (Swagger)
 * Slack: #pdl
 * Teamkatalog: https://teamkatalogen.nav.no/team/034cbcd2-ac28-4e2e-88c8-345945933f70
 *
 * Spørringen henter både gjeldende og historiske folkeregisteridenter, slik at vi finner tiltaksdeltakelser registrert på et tidligere fødselsnummer.
 * Behandlingsnummeret B470 er tiltakspengers oppføring i behandlingskatalogen: https://behandlingskatalog.intern.nav.no/process/purpose/TILTAKSPENGER/7b1ef0b2-9d17-413e-8bc3-0efed8adc623
 *
 * Klienten logger ikke selv; feillogging skjer én gang i [no.nav.tiltakspenger.tiltak.services.TiltakshistorikkService], som har domenekonteksten.
 * Klienten har ingen retry, slik den gamle ktor-klienten heller ikke hadde ([Retry.Ingen] er default, men skrives ut for å gjøre pariteten synlig).
 * Statusregelen er [Statusregel.Alle2xx] for å bevare den gamle `isSuccess`-sjekken.
 *
 * **Tidsbudsjett — les sammen med [no.nav.tiltakspenger.tiltak.clients.tiltakshistorikk.TiltakshistorikkClient].**
 * Et innkommende kall til tiltak gjør to oppslag etter hverandre, og summen skal lande på rundt 30 sekunder i verste fall.
 * Dette oppslaget er det billigste av de to — én ident inn, identer ut, ingen sammenstilling — og får derfor 5 sekunder uten retry.
 * Resten av budsjettet går til tiltakshistorikk, som venter på Arena og de andre kildesystemene.
 *
 * @param timeout Per-kall timeout, se tidsbudsjettet over; ktor-oppsettets 60 sekunder var en felles blankoverdi for alle klientene, ikke en vurdering for dette oppslaget.
 * @param transport Det eneste stedet klienten rører nettverket; default er produksjonstransporten, tester sender inn `FakeHttpTransport` slik at hele den reelle pipelinen kjører.
 */
class PdlClient(
    baseUrl: String,
    clock: Clock,
    authTokenProvider: AuthTokenProvider,
    connectTimeout: Duration = 2.seconds,
    timeout: Duration = 5.seconds,
    transport: HttpTransport = JavaHttpTransport(connectTimeout = connectTimeout),
) {
    private val httpKlient: HttpKlient = HttpKlient(
        clock = clock,
        config = HttpKlientConfig(
            timeout = timeout,
            auth = KlientAuth.System(authTokenProvider),
            retry = Retry.Ingen,
        ),
        transport = transport,
    )

    private val graphqlUri = URI.create("$baseUrl/graphql")

    /**
     * Henter nåværende og historiske folkeregisteridenter for [fnr].
     * Venstresiden skiller mellom et kall som feilet ([KanIkkeHentePerson.KallFeilet]) og et svar vi ikke fikk identer ut av; kalleren avgjør hva som skal føre til feil utad.
     */
    suspend fun hentNåværendeOgHistoriskeFødselsnummer(fnr: String): Either<KanIkkeHentePerson, List<String>> {
        return httpKlient.postJson<GraphQLResponse<HentIdenterResponse>>(
            uri = graphqlUri,
            body = HentIdenterRequest(
                query = hentIdenterQuery,
                variables = PdlVariables(ident = fnr),
            ),
            headere = listOf(
                NavHeadere.tema("IND"),
                NavHeadere.behandlingsnummer("B470"),
            ),
            godta = Statusregel.Alle2xx,
        ).mapLeft {
            KanIkkeHentePerson.KallFeilet(it)
        }.flatMap { respons ->
            respons.body.tilIdenter(respons.metadata)
        }
    }

    /**
     * GraphQL svarer av design 200 OK på alle svar; funksjonelle feil ligger i errors-lista.
     * PDL svarer også `data: null` sammen med errors for enkelte feilkoder, derfor er begge deler nullable her.
     */
    private fun GraphQLResponse<HentIdenterResponse>.tilIdenter(
        metadata: HttpKlientMetadata,
    ): Either<KanIkkeHentePerson, List<String>> {
        val graphQLFeil = errors.orEmpty()
        if (graphQLFeil.isNotEmpty()) {
            return KanIkkeHentePerson.GraphQLFeil(
                feilmeldinger = graphQLFeil.map { it.message ?: "ukjent" },
                metadata = metadata,
            ).left()
        }
        val identer = data?.hentIdenter?.identer.orEmpty()
        if (identer.isEmpty()) {
            return KanIkkeHentePerson.FantIngenIdenter(metadata).left()
        }
        return identer.map { it.ident }.right()
    }
}

data class HentIdenterRequest(val query: String, val variables: PdlVariables)

data class PdlVariables(val ident: String) {
    /** Identen er et fødselsnummer, så den maskeres i logger og feilmeldinger. */
    override fun toString() = "PdlVariables(ident=*****)"
}

data class HentIdenterResponse(
    val hentIdenter: Identliste?,
)

data class Identliste(
    val identer: List<IdentInformasjon>,
)

data class IdentInformasjon(
    val ident: String,
)

private val hentIdenterQuery = $$"""
    query($ident: ID!){
      hentIdenter(ident: $ident, grupper: FOLKEREGISTERIDENT, historikk: true) {
          identer {
            ident
          }
        }
    }
""".trimIndent()
