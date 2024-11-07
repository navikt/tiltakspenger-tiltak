package no.nav.tiltakspenger.tiltak

import no.nav.tiltakspenger.tiltak.auth.AzureTokenProvider
import no.nav.tiltakspenger.tiltak.clients.arena.ArenaClient
import no.nav.tiltakspenger.tiltak.clients.arena.ArenaClientImpl
import no.nav.tiltakspenger.tiltak.clients.komet.KometClientImpl
import no.nav.tiltakspenger.tiltak.services.RouteServiceImpl
import no.nav.tiltakspenger.tiltak.services.RoutesService

internal class ApplicationBuilder {
    val tokenProviderKomet: AzureTokenProvider = AzureTokenProvider(config = Configuration.oauthConfigKomet())
    val tokenProviderArena: AzureTokenProvider = AzureTokenProvider(config = Configuration.oauthConfigArena())
    val kometClient: KometClientImpl = KometClientImpl(
        getToken = tokenProviderKomet::getToken,
    )
    val arenaClient: ArenaClient = ArenaClientImpl(
        getToken = tokenProviderArena::getToken,
    )
    val routesService: RoutesService = RouteServiceImpl(
        kometClient = kometClient,
        arenaClient = arenaClient,
    )
}
