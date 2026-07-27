package no.nav.tiltakspenger.tiltak.testutils

import no.nav.tiltakspenger.libs.common.AccessToken
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.texas.IdentityProvider
import no.nav.tiltakspenger.libs.texas.client.TexasClient
import no.nav.tiltakspenger.libs.texas.client.TexasIntrospectionResponse
import java.time.Instant

/**
 * Fake av [TexasClient] for rute-testene, i stedet for en mock.
 * Faken er uten tilstand — svaret er gitt av konstruktørargumentene — så to tester kan bruke hver sin instans (eller samme) uten å påvirke hverandre.
 *
 * @param pid Fødselsnummeret tokenx-principalen får; `null` for azure-tokens.
 * @param navIdent NAVident-claimet azure-principalen får; `null` for tokenx-tokens.
 * @param aktiv `false` simulerer et utløpt/avvist token, som gir `401` fra `TexasAuthenticationProvider`.
 */
class TexasClientFake(
    private val pid: String? = null,
    private val navIdent: String? = null,
    private val aktiv: Boolean = true,
    private val klientnavn: String = "test-klient",
) : TexasClient {
    override suspend fun introspectToken(
        token: String,
        identityProvider: IdentityProvider,
    ): TexasIntrospectionResponse = TexasIntrospectionResponse(
        active = aktiv,
        error = if (aktiv) null else "Expired",
        groups = null,
        roles = null,
        other = if (!aktiv) {
            emptyMap()
        } else {
            buildMap {
                put("azp_name", klientnavn)
                put("azp", "$klientnavn-id")
                if (pid != null) {
                    put("pid", pid)
                    put("acr", "idporten-loa-high")
                }
                if (navIdent != null) put("NAVident", navIdent)
            }
        },
    )

    override suspend fun getSystemToken(
        audienceTarget: String,
        identityProvider: IdentityProvider,
        rewriteAudienceTarget: Boolean,
        skipCache: Boolean,
    ): AccessToken = testToken

    override suspend fun exchangeToken(
        userToken: String,
        audienceTarget: String,
        identityProvider: IdentityProvider,
        skipCache: Boolean,
    ): AccessToken = testToken
}

private val testToken = AccessToken("token", Instant.now(fixedClock).plusSeconds(3600))
