package no.nav.tiltakspenger.tiltak

import io.github.oshai.kotlinlogging.KLogger
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.SessionCounter
import no.nav.tiltakspenger.libs.texas.IdentityProvider
import no.nav.tiltakspenger.libs.texas.client.TexasClient
import no.nav.tiltakspenger.libs.texas.client.TexasHttpClient
import no.nav.tiltakspenger.tiltak.clients.arena.ArenaClient
import no.nav.tiltakspenger.tiltak.clients.arena.ArenaClientImpl
import no.nav.tiltakspenger.tiltak.clients.komet.KometClientImpl
import no.nav.tiltakspenger.tiltak.db.DataSourceSetup
import no.nav.tiltakspenger.tiltak.gjennomforing.db.GjennomforingRepo
import no.nav.tiltakspenger.tiltak.gjennomforing.kafka.GjennomforingConsumer
import no.nav.tiltakspenger.tiltak.gjennomforing.tiltakstype.db.TiltakstypeRepo
import no.nav.tiltakspenger.tiltak.gjennomforing.tiltakstype.kafka.TiltakstypeConsumer
import no.nav.tiltakspenger.tiltak.services.RouteServiceImpl
import no.nav.tiltakspenger.tiltak.services.RoutesService
import no.nav.tiltakspenger.tiltak.testdata.KometTestdataClient

class ApplicationContext(log: KLogger) {
    val dataSource = DataSourceSetup.createDatasource(Configuration.jdbcUrl)
    val sessionCounter = SessionCounter(log)
    val sessionFactory = PostgresSessionFactory(dataSource, sessionCounter)

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

    val tiltakstypeRepo = TiltakstypeRepo(sessionFactory)
    val tiltakstypeConsumer = TiltakstypeConsumer(
        tiltakstypeRepo = tiltakstypeRepo,
        topic = Configuration.tiltakstypeTopic,
    )
    val gjennomforingRepo = GjennomforingRepo(sessionFactory)
    val gjennomforingConsumer = GjennomforingConsumer(
        gjennomforingRepo = gjennomforingRepo,
        topic = Configuration.gjennomforingTopic,
    )
}
