package no.nav.tiltakspenger.tiltak.routes

import io.kotest.matchers.shouldBe
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.path
import io.ktor.server.testing.testApplication
import io.ktor.server.util.url
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequest
import no.nav.tiltakspenger.libs.texas.IdentityProvider
import no.nav.tiltakspenger.libs.texas.client.TexasClient
import no.nav.tiltakspenger.libs.texas.client.TexasIntrospectionResponse
import no.nav.tiltakspenger.tiltak.services.RoutesService
import no.nav.tiltakspenger.tiltak.services.TiltakshistorikkService
import no.nav.tiltakspenger.tiltak.setupTestApplication
import org.junit.jupiter.api.Test

class TokenxRoutesTest {

    private val texasClient = mockk<TexasClient>()
    private val mockRoutesService = mockk<RoutesService>()
    private val mockTiltakshistorikkService = mockk<TiltakshistorikkService>()

    @Test
    fun `get tiltak tokenx - utløpt token - returnerer 401`() {
        coEvery { texasClient.introspectToken(any(), IdentityProvider.TOKENX) } returns TexasIntrospectionResponse(
            active = false,
            error = "Expired",
            groups = null,
            roles = null,
            other = emptyMap(),
        )
        runTest {
            testApplication {
                application {
                    setupTestApplication(mockRoutesService, texasClient, mockTiltakshistorikkService)
                }
                defaultRequest(
                    HttpMethod.Get,
                    url {
                        protocol = URLProtocol.HTTPS
                        path("/tokenx/tiltak")
                    },
                ).apply {
                    status shouldBe HttpStatusCode.Unauthorized
                }
            }
        }
    }

    @Test
    fun `get tiltak tokenx - gyldig token - returnerer ok respons`() {
        val fnr = "12345678910"
        coEvery { texasClient.introspectToken(any(), IdentityProvider.TOKENX) } returns TexasIntrospectionResponse(
            active = true,
            error = null,
            groups = null,
            roles = null,
            other = mapOf(
                "azp_name" to "soknad-api",
                "azp" to "soknad-api-id",
                "acr" to "idporten-loa-high",
                "pid" to fnr,
            ),
        )
        coEvery { mockRoutesService.hentTiltakForSøknad(fnr, any()) } returns emptyList()
        runTest {
            testApplication {
                application {
                    setupTestApplication(mockRoutesService, texasClient, mockTiltakshistorikkService)
                }
                defaultRequest(
                    HttpMethod.Get,
                    url {
                        protocol = URLProtocol.HTTPS
                        path("/tokenx/tiltak")
                    },
                ).apply {
                    status shouldBe HttpStatusCode.OK
                }
            }
        }
    }

    @Test
    fun `get tiltakshistorikk tokenx - utløpt token - returnerer 401`() {
        coEvery { texasClient.introspectToken(any(), IdentityProvider.TOKENX) } returns TexasIntrospectionResponse(
            active = false,
            error = "Expired",
            groups = null,
            roles = null,
            other = emptyMap(),
        )
        runTest {
            testApplication {
                application {
                    setupTestApplication(mockRoutesService, texasClient, mockTiltakshistorikkService)
                }
                defaultRequest(
                    HttpMethod.Get,
                    url {
                        protocol = URLProtocol.HTTPS
                        path("/tokenx/tiltakshistorikk")
                    },
                ).apply {
                    status shouldBe HttpStatusCode.Unauthorized
                }
            }
        }
    }

    @Test
    fun `get tiltakshistorikk tokenx - gyldig token - returnerer ok respons`() {
        val fnr = "12345678910"
        coEvery { texasClient.introspectToken(any(), IdentityProvider.TOKENX) } returns TexasIntrospectionResponse(
            active = true,
            error = null,
            groups = null,
            roles = null,
            other = mapOf(
                "azp_name" to "soknad-api",
                "azp" to "soknad-api-id",
                "acr" to "idporten-loa-high",
                "pid" to fnr,
            ),
        )
        coEvery { mockTiltakshistorikkService.hentTiltakshistorikkForSoknad(fnr, any()) } returns emptyList()
        runTest {
            testApplication {
                application {
                    setupTestApplication(mockRoutesService, texasClient, mockTiltakshistorikkService)
                }
                defaultRequest(
                    HttpMethod.Get,
                    url {
                        protocol = URLProtocol.HTTPS
                        path("/tokenx/tiltakshistorikk")
                    },
                ).apply {
                    status shouldBe HttpStatusCode.OK
                }
            }
        }
    }
}
