package no.nav.tiltakspenger.routes

import com.nimbusds.jwt.SignedJWT
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.mockk.mockk
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.tiltakspenger.tiltak.jacksonSerialization
import no.nav.tiltakspenger.tiltak.routes.routes
import no.nav.tiltakspenger.tiltak.services.RoutesService
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class Routes_ktKtTest {
    private val routeServiceMock = mockk<RoutesService>()
    private val mockOAuth2Server = MockOAuth2Server()

    @BeforeAll
    fun setup() = mockOAuth2Server.start(8080)

    @AfterAll
    fun after() = mockOAuth2Server.shutdown()

    fun testToken(expiry: Long = 3600): SignedJWT {
        return mockOAuth2Server.issueToken(
            issuerId = "",
            audience = "e",
            claims = mapOf(
                "pid" to "",
            ),
            expiry = expiry,
        )
    }

    @Test
    fun `teste med gyldig token`() {
        val token = "Bearer ugyldig blah blah"
        testApplication {
            application {
                jacksonSerialization()
                routing {}
            }
            val response = client.get("/tiltak") {
                header("Authorization", "Bearer ugyldigtoken")
            }
            Assertions.assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
    }
}
