package no.nav.tiltakspenger.tiltak

import no.nav.tiltakspenger.libs.texas.IdentityProvider
import no.nav.tiltakspenger.libs.texas.client.TexasClient
import no.nav.tiltakspenger.libs.texas.client.TexasHttpClient
import no.nav.tiltakspenger.tiltak.clients.arena.ArenaClient
import no.nav.tiltakspenger.tiltak.clients.arena.ArenaClientImpl
import no.nav.tiltakspenger.tiltak.clients.komet.KometClientImpl
import no.nav.tiltakspenger.tiltak.services.RouteServiceImpl
import no.nav.tiltakspenger.tiltak.services.RoutesService
import no.nav.tiltakspenger.tiltak.testdata.KometTestdataClient

internal class ApplicationBuilder {
    val texasClient: TexasClient = TexasHttpClient(
        introspectionUrl = Configuration.naisTokenIntrospectionEndpoint,
        tokenUrl = Configuration.naisTokenEndpoint,
        tokenExchangeUrl = Configuration.tokenExchangeEndpoint,
    )
    val kometClient: KometClientImpl = KometClientImpl(
        baseUrl = Configuration.kometUrl,
        getToken = { texasClient.getSystemToken(Configuration.kometScope, IdentityProvider.AZUREAD, rewriteAudienceTarget = false) },
    )
    val arenaClient: ArenaClient = ArenaClientImpl(
        baseUrl = Configuration.arenaUrl,
        getToken = { texasClient.getSystemToken(Configuration.arenaScope, IdentityProvider.AZUREAD, rewriteAudienceTarget = false) },
    )
    val routesService: RoutesService = RouteServiceImpl(
        kometClient = kometClient,
        arenaClient = arenaClient,
    )
    val kometTestdataClient = KometTestdataClient(
        kometTestdataEndpoint = Configuration.kometTestdataUrl,
        getToken = { texasClient.getSystemToken(Configuration.kometTestdataScope, IdentityProvider.AZUREAD, rewriteAudienceTarget = false) },
    )
}
