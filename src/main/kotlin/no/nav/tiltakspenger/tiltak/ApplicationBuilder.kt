package no.nav.tiltakspenger.tiltak

import mu.KotlinLogging
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.tiltakspenger.tiltak.auth.AzureTokenProvider
import no.nav.tiltakspenger.tiltak.clients.arena.ArenaClient
import no.nav.tiltakspenger.tiltak.clients.arena.ArenaClientImpl
import no.nav.tiltakspenger.tiltak.clients.komet.KometClientImpl
import no.nav.tiltakspenger.tiltak.clients.tiltak.TiltakClient
import no.nav.tiltakspenger.tiltak.clients.tiltak.TiltakClientImpl
import no.nav.tiltakspenger.tiltak.clients.valp.ValpClient
import no.nav.tiltakspenger.tiltak.clients.valp.ValpClientImpl
import no.nav.tiltakspenger.tiltak.services.KometService
import no.nav.tiltakspenger.tiltak.services.RouteServiceImpl
import no.nav.tiltakspenger.tiltak.services.RoutesService
import no.nav.tiltakspenger.tiltak.services.TiltakService

private val LOG = KotlinLogging.logger {}

internal class ApplicationBuilder(
    tokenProviderKomet: AzureTokenProvider = AzureTokenProvider(config = Configuration.oauthConfigKomet()),
    tokenProviderValp: AzureTokenProvider = AzureTokenProvider(config = Configuration.oauthConfigValp()),
    tokenProviderTiltak: AzureTokenProvider = AzureTokenProvider(config = Configuration.oauthConfigTiltak()),
    tokenProviderArena: AzureTokenProvider = AzureTokenProvider(config = Configuration.oauthConfigArena()),
    kometClient: KometClientImpl = KometClientImpl(
        getToken = tokenProviderKomet::getToken,
    ),
    valpClient: ValpClient = ValpClientImpl(
        getToken = tokenProviderValp::getToken,
    ),
    arenaClient: ArenaClient = ArenaClientImpl(
        getToken = tokenProviderArena::getToken,
    ),
    tiltakClient: TiltakClient = TiltakClientImpl(
        getToken = tokenProviderTiltak::getToken,
    ),
    routesService: RoutesService = RouteServiceImpl(
        kometClient = kometClient,
        valpClient = valpClient,
        tiltakClient = tiltakClient,
        arenaClient = arenaClient,
    ),
) : RapidsConnection.StatusListener {
    private val rapidsConnection: RapidsConnection = RapidApplication.Builder(
        RapidApplication.RapidApplicationConfig.fromEnv(Configuration.rapidsAndRivers),
    )
        .withKtorModule {
            tiltakApi(routesService)
        }
        .build()
        .apply {
            TiltakService(
                rapidsConnection = this,
                routesService = routesService,
            )

            KometService(
                rapidsConnection = this,
            )
        }

    init {
        rapidsConnection.register(this)
    }

    fun start() {
        rapidsConnection.start()
    }

    override fun onStartup(rapidsConnection: RapidsConnection) {
        LOG.info { "Starting tiltakspenger-tiltak" }
    }

    override fun onShutdown(rapidsConnection: RapidsConnection) {
        LOG.info { "Stopping tiltakspenger-tiltak" }
        super.onShutdown(rapidsConnection)
    }
}
