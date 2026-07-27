package no.nav.tiltakspenger.tiltak

import no.nav.tiltakspenger.libs.logging.Sikkerlogg
import no.nav.tiltakspenger.libs.logging.infra.KotlinLoggingSikkerlogg
import no.nav.tiltakspenger.libs.texas.client.TexasClient
import no.nav.tiltakspenger.libs.texas.client.TexasHttpClient
import no.nav.tiltakspenger.libs.texas.client.TexasSystemTokenProvider
import no.nav.tiltakspenger.tiltak.clients.tiltakshistorikk.TiltakshistorikkClient
import no.nav.tiltakspenger.tiltak.person.infra.http.pdl.PdlClient
import no.nav.tiltakspenger.tiltak.services.TiltakshistorikkService
import java.time.Clock

class ApplicationContext(clock: Clock) {
    val texasClient: TexasClient = TexasHttpClient(
        introspectionUrl = Configuration.naisTokenIntrospectionEndpoint,
        tokenUrl = Configuration.naisTokenEndpoint,
        tokenExchangeUrl = Configuration.tokenExchangeEndpoint,
        clock = clock,
    )

    /** Appens egen sikkerlogg-instans, slik at feilloggene får en klikkbar lenke til sikkerloggen i GCP. */
    val sikkerlogg: Sikkerlogg = KotlinLoggingSikkerlogg(
        appNavn = Configuration.naisAppName,
        gcpProsjektId = Configuration.gcpTeamProjectId,
    )

    private fun systemTokenProvider(scope: String) = TexasSystemTokenProvider(
        texasClient = texasClient,
        audienceTarget = scope,
    )

    val pdlClient = PdlClient(
        baseUrl = Configuration.pdlUrl,
        clock = clock,
        authTokenProvider = systemTokenProvider(Configuration.pdlScope),
    )

    val tiltakshistorikkClient: TiltakshistorikkClient = TiltakshistorikkClient(
        baseUrl = Configuration.tiltakshistorikkUrl,
        clock = clock,
        authTokenProvider = systemTokenProvider(Configuration.tiltakshistorikkScope),
    )

    val tiltakshistorikkService: TiltakshistorikkService = TiltakshistorikkService(
        tiltakshistorikkClient = tiltakshistorikkClient,
        pdlClient = pdlClient,
        clock = clock,
        sikkerlogg = sikkerlogg,
    )
}
