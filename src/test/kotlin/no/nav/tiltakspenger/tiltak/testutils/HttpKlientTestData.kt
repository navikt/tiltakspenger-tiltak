package no.nav.tiltakspenger.tiltak.testutils

import no.nav.tiltakspenger.libs.common.AccessToken
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.httpklient.infra.kall.AuthTokenProvider
import java.time.Instant

/** Systemtoken-provider for klienttestene; tokenet er gyldig lenge nok til at klienten aldri henter et nytt. */
val testTokenProvider = object : AuthTokenProvider {
    override suspend fun hentToken(skipCache: Boolean) = AccessToken("token", Instant.now(fixedClock).plusSeconds(3600))
}
