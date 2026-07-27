package no.nav.tiltakspenger.tiltak.routes

import io.kotest.matchers.shouldBe
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.httpklient.infra.kall.HttpMethod
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetBody
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequestWithAssertions
import no.nav.tiltakspenger.tiltak.clients.tiltakshistorikk.dto.KometDeltakerStatusDto
import no.nav.tiltakspenger.tiltak.setupTestApplication
import no.nav.tiltakspenger.tiltak.testutils.TexasClientFake
import no.nav.tiltakspenger.tiltak.testutils.TiltakTestkontekst
import no.nav.tiltakspenger.tiltak.testutils.tiltakshistorikkKometTiltak
import org.junit.jupiter.api.Test

/**
 * Ende-til-ende mot ekte service og klienter; kun nettverket (`FakeHttpTransport`) og Texas er byttet ut med fakes.
 * Alt bygges inne i hver test, så testene deler ingen tilstand og kan kjøre parallelt.
 */
class TokenxRoutesTest {

    @Test
    fun `get tiltakshistorikk tokenx - utløpt token - returnerer 401`() = runTest {
        val kontekst = TiltakTestkontekst()
        testApplication {
            application {
                setupTestApplication(TexasClientFake(aktiv = false), kontekst.tiltakshistorikkService)
            }
            defaultRequestWithAssertions(
                HttpMethod.GET,
                "/tokenx/tiltakshistorikk",
                forventet = ForventetRespons(status = 401),
            )
        }
        // Uten gyldig token skal ingen oppslag gjøres.
        kontekst.pdlTransport.mottatteKall.size shouldBe 0
        kontekst.tiltakshistorikkTransport.mottatteKall.size shouldBe 0
    }

    @Test
    fun `get tiltakshistorikk tokenx - gyldig token - returnerer tiltakene brukeren kan søke på`() = runTest {
        val kontekst = TiltakTestkontekst()
        val deltakelse = tiltakshistorikkKometTiltak(
            status = KometDeltakerStatusDto(type = KometDeltakerStatusDto.DeltakerStatusType.DELTAR),
        )
        kontekst.køOppslag(listOf(deltakelse))

        testApplication {
            application {
                setupTestApplication(TexasClientFake(pid = kontekst.fnr), kontekst.tiltakshistorikkService)
            }
            defaultRequestWithAssertions(
                HttpMethod.GET,
                "/tokenx/tiltakshistorikk",
                forventet = ForventetRespons(status = 200),
            )
        }
        // Oppslaget gikk mot PDL først, deretter tiltakshistorikk med identene PDL ga oss.
        kontekst.pdlTransport.mottatteKall.size shouldBe 1
        kontekst.tiltakshistorikkTransport.mottatteKall.size shouldBe 1
    }

    /** Feilet oppslag ga 500 også før migreringen til `httpklient`, da klientene kastet i stedet for å returnere Either. */
    @Test
    fun `get tiltakshistorikk tokenx - feilet oppslag mot tiltakshistorikk - returnerer 500`() = runTest {
        val kontekst = TiltakTestkontekst()
        kontekst.køPdlIdenter(listOf(kontekst.fnr))
        kontekst.tiltakshistorikkTransport.leggIKøStatusForAlleForsøk(statusCode = 500, body = "kaboom", maksForsøk = 3)

        testApplication {
            application {
                setupTestApplication(TexasClientFake(pid = kontekst.fnr), kontekst.tiltakshistorikkService)
            }
            defaultRequestWithAssertions(
                HttpMethod.GET,
                "/tokenx/tiltakshistorikk",
                forventet = ForventetRespons.json(500, """{"melding":"Noe gikk galt på serversiden","kode":"server_feil"}"""),
            )
        }
    }
}
