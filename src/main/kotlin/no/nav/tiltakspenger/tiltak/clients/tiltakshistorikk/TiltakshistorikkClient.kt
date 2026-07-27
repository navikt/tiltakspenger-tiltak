package no.nav.tiltakspenger.tiltak.clients.tiltakshistorikk

import arrow.core.Either
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.infra.HttpKlient
import no.nav.tiltakspenger.libs.httpklient.infra.HttpKlientConfig
import no.nav.tiltakspenger.libs.httpklient.infra.kall.AuthTokenProvider
import no.nav.tiltakspenger.libs.httpklient.infra.kall.KlientAuth
import no.nav.tiltakspenger.libs.httpklient.infra.kall.Statusregel
import no.nav.tiltakspenger.libs.httpklient.infra.retry.Retry
import no.nav.tiltakspenger.libs.httpklient.infra.transport.HttpTransport
import no.nav.tiltakspenger.libs.httpklient.infra.transport.JavaHttpTransport
import no.nav.tiltakspenger.tiltak.clients.tiltakshistorikk.dto.NorskIdent
import no.nav.tiltakspenger.tiltak.clients.tiltakshistorikk.dto.TiltakshistorikkV1Dto
import no.nav.tiltakspenger.tiltak.clients.tiltakshistorikk.dto.TiltakshistorikkV1Request
import no.nav.tiltakspenger.tiltak.clients.tiltakshistorikk.dto.TiltakshistorikkV1Response
import java.net.URI
import java.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Tjeneste som tilbys av Team Valp som leverer tiltaksdeltakelser uavhengig av kildesystem (kan være Arena, Komet eller Team Tiltak)
 *
 * Kildekode: https://github.com/navikt/mulighetsrommet/tree/main/mulighetsrommet-tiltakshistorikk
 * Dokumentasjon: README-en i kildekode-mappa
 * API-spec: - (ingen OpenAPI-spec; Team Valp tilbyr en typet Kotlin-klient i https://github.com/navikt/mulighetsrommet/tree/main/common/tiltakshistorikk-client)
 * Slack: #team-valp
 * Teamkatalog: https://teamkatalogen.nav.no/team/aa730c95-b437-497b-b1ae-0ccf69a10997
 *
 * Klienten logger ikke selv; feillogging skjer én gang i [no.nav.tiltakspenger.tiltak.services.TiltakshistorikkService], som har domenekonteksten.
 * [Retry.Fast.retryIkkeIdempotente] er satt fordi historikk-oppslaget går som POST, men er et rent leseoppslag — identer inn, historikk ut, ingen sideeffekter hos Team Valp.
 * Uten flagget ville migreringen stille fjernet retryen POST-en faktisk har i dag, siden `httpklient` ellers aldri retryer POST.
 * Tiltakshistorikk godtar kun `200` som suksess, slik den gamle klienten også gjorde.
 *
 * **Tidsbudsjett — les sammen med [no.nav.tiltakspenger.tiltak.person.infra.http.pdl.PdlClient].**
 * Et innkommende kall til tiltak gjør to oppslag etter hverandre: først PDL (maks 5 s), så tiltakshistorikk.
 * Summen skal lande på rundt 30 sekunder i verste fall, og ingen enkeltforsøk skal vare mer enn 10 sekunder.
 * Regnestykket her er 3 forsøk × 7 s + 2 × 100 ms backoff = 21,2 s, som med PDLs 5 s gir ~26 s og lar ~4 s stå igjen til Texas-token, introspeksjon og mapping.
 * Retryen er derfor kuttet fra ktor-klientens fire forsøk til tre — et bevisst avvik fra pariteten, ikke en forglemmelse.
 * Det koster lite: retry hjelper mot raske feil (`5xx`, avvist tilkobling), som svarer på millisekunder og aldri spiser timeouten.
 * `httpklient` har ingen ekte deadline over hele kallet ennå, så budsjettet er summert for hånd her; se punktet om makstimeout i metarepoets `TASKS.md`.
 *
 * @param timeout Per-forsøk timeout, ikke totalbudsjett — se tidsbudsjettet over.
 * Tiltakshistorikk sammenstiller flere kildesystemer (Arena er den tregeste), men det hjelper oss ikke å vente lenger enn konsumentene våre: soknad-api gir opp etter 10 sekunder og saksbehandling-api etter 60.
 * Ktor-oppsettets 60 sekunder var en felles blankoverdi for alle klientene, ikke en vurdering for dette endepunktet.
 * @param transport Det eneste stedet klienten rører nettverket; default er produksjonstransporten, tester sender inn `FakeHttpTransport` slik at hele den reelle pipelinen kjører.
 */
class TiltakshistorikkClient(
    baseUrl: String,
    clock: Clock,
    authTokenProvider: AuthTokenProvider,
    connectTimeout: Duration = 2.seconds,
    timeout: Duration = 7.seconds,
    transport: HttpTransport = JavaHttpTransport(connectTimeout = connectTimeout),
) {
    private val httpKlient: HttpKlient = HttpKlient(
        clock = clock,
        config = HttpKlientConfig(
            timeout = timeout,
            auth = KlientAuth.System(authTokenProvider),
            retry = Retry.Fast(maksForsøk = 3, delay = 100.milliseconds, retryIkkeIdempotente = true),
        ),
        transport = transport,
    )

    private val historikkUri = URI.create("$baseUrl/api/v1/historikk")

    suspend fun hentTiltaksdeltakelser(fnr: List<String>): Either<HttpKlientError, List<TiltakshistorikkV1Dto>> {
        return httpKlient.postJson<TiltakshistorikkV1Response>(
            uri = historikkUri,
            body = TiltakshistorikkV1Request(
                identer = fnr.map { NorskIdent(it) },
            ),
            godta = Statusregel.Eksakt(200),
        ).map { respons -> respons.body.historikk }
    }
}
