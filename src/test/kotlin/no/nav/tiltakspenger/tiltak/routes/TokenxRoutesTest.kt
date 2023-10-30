package no.nav.tiltakspenger.tiltak.routes

import com.nimbusds.jwt.SignedJWT
import io.mockk.mockk
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.tiltakspenger.tiltak.services.RoutesService
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll

class TokenxRoutesTest {

    private val mockOAuth2Server = MockOAuth2Server()
    private val mockRoutesService = mockk<RoutesService>()

    @BeforeAll
    fun setup() = mockOAuth2Server.start(8080)

    @AfterAll
    fun after() = mockOAuth2Server.shutdown()

    private fun testToken(expiry: Long = 3600): SignedJWT {
        return mockOAuth2Server.issueToken(
            issuerId = "default",
            audience = "xxx",
            claims = mapOf(
                "pid" to "123",
            ),
            expiry = expiry,
        )
    }

//    @Test
//    fun `sjekk at kall med gyldig token går ok`() {
//        val token = testToken()
//
//        testApplication {
//            application {
//                // vedtakTestApi()
//                // installAuthentication()
//
//                setupRouting(routesService = mockRoutesService)
//
//            }
//
//            val response = client.get("/tokenx/tiltak") {
//                contentType(type = ContentType.Application.Json)
//                header("Authorization", "Bearer ${token.serialize()}")
//            }
//
//            response.status shouldBe HttpStatusCode.OK
//        }
//    }
//
//    @Test
//    fun `sjekk at kall med ugyldig token gir feilmelding`() {
//
//    }
}
