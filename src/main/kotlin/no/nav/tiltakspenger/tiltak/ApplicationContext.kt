package no.nav.tiltakspenger.tiltak

import no.nav.tiltakspenger.libs.texas.IdentityProvider
import no.nav.tiltakspenger.libs.texas.client.TexasClient
import no.nav.tiltakspenger.libs.texas.client.TexasHttpClient
import no.nav.tiltakspenger.tiltak.clients.tiltakshistorikk.TiltakshistorikkClient
import no.nav.tiltakspenger.tiltak.services.TiltakshistorikkService
import no.nav.tiltakspenger.tiltak.testdata.KometTestdataClient

class ApplicationContext {
    val texasClient: TexasClient = TexasHttpClient(
        introspectionUrl = Configuration.naisTokenIntrospectionEndpoint,
        tokenUrl = Configuration.naisTokenEndpoint,
        tokenExchangeUrl = Configuration.tokenExchangeEndpoint,
    )

    val tiltakshistorikkClient: TiltakshistorikkClient = TiltakshistorikkClient(
        baseUrl = Configuration.tiltakshistorikkUrl,
        getToken = { texasClient.getSystemToken(Configuration.tiltakshistorikkScope, IdentityProvider.AZUREAD, rewriteAudienceTarget = false) },
    )

    val tiltakshistorikkService: TiltakshistorikkService = TiltakshistorikkService(
        tiltakshistorikkClient = tiltakshistorikkClient,
    )
    val kometTestdataClient = KometTestdataClient(
        kometTestdataEndpoint = Configuration.kometTestdataUrl,
        getToken = { texasClient.getSystemToken(Configuration.kometTestdataScope, IdentityProvider.AZUREAD, rewriteAudienceTarget = false) },
    )
}
