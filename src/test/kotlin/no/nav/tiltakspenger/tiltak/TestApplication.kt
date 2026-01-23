package no.nav.tiltakspenger.tiltak

import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.routing
import no.nav.tiltakspenger.libs.texas.IdentityProvider
import no.nav.tiltakspenger.libs.texas.client.TexasClient
import no.nav.tiltakspenger.tiltak.routes.azureRoutes
import no.nav.tiltakspenger.tiltak.routes.tokenxRoutes
import no.nav.tiltakspenger.tiltak.services.TiltakshistorikkService

fun Application.setupTestApplication(
    texasClient: TexasClient,
    tiltakshistorikkService: TiltakshistorikkService,
) {
    jacksonSerialization()
    installAuthentication(texasClient)
    routing {
        authenticate(IdentityProvider.TOKENX.value) {
            tokenxRoutes(tiltakshistorikkService)
        }
        authenticate(IdentityProvider.AZUREAD.value) {
            azureRoutes(tiltakshistorikkService)
        }
    }
}
