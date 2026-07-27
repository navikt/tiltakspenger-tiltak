package no.nav.tiltakspenger.tiltak.routes

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.httpklient.infra.kall.HttpMethod
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
class AzureRoutesTest {

    @Test
    fun `post tiltakshistorikk azure - gyldig token - returnerer tiltakene`() = runTest {
        val kontekst = TiltakTestkontekst()
        val historiskFnr = "11111111111"
        kontekst.køOppslag(
            deltakelser = listOf(
                tiltakshistorikkKometTiltak(
                    status = KometDeltakerStatusDto(type = KometDeltakerStatusDto.DeltakerStatusType.DELTAR),
                ),
            ),
            identer = listOf(kontekst.fnr, historiskFnr),
        )

        testApplication {
            application {
                setupTestApplication(TexasClientFake(navIdent = "Z12345"), kontekst.tiltakshistorikkService)
            }
            defaultRequestWithAssertions(
                HttpMethod.POST,
                "/azure/tiltakshistorikk",
                body = """{"ident": "${kontekst.fnr}"}""",
                forventet = ForventetRespons(status = 200),
            )
        }
        // Historiske identer fra PDL blir med i oppslaget mot tiltakshistorikk.
        kontekst.tiltakshistorikkTransport.mottatteKall.single().bodyTekst shouldContain historiskFnr
    }

    @Test
    fun `post tiltakshistorikk azure - tomt svar - returnerer tom liste`() = runTest {
        val kontekst = TiltakTestkontekst()
        kontekst.køOppslag()

        testApplication {
            application {
                setupTestApplication(TexasClientFake(navIdent = "Z12345"), kontekst.tiltakshistorikkService)
            }
            defaultRequestWithAssertions(
                HttpMethod.POST,
                "/azure/tiltakshistorikk",
                body = """{"ident": "${kontekst.fnr}"}""",
                forventet = ForventetRespons.json(200, "[]"),
            )
        }
    }

    /** Feilet oppslag ga 500 også før migreringen til `httpklient`, da klientene kastet i stedet for å returnere Either. */
    @Test
    fun `post tiltakshistorikk azure - feilet oppslag mot PDL - returnerer 500`() = runTest {
        val kontekst = TiltakTestkontekst()
        kontekst.pdlTransport.leggIKøStatus(statusCode = 500, body = "kaboom")

        testApplication {
            application {
                setupTestApplication(TexasClientFake(navIdent = "Z12345"), kontekst.tiltakshistorikkService)
            }
            defaultRequestWithAssertions(
                HttpMethod.POST,
                "/azure/tiltakshistorikk",
                body = """{"ident": "${kontekst.fnr}"}""",
                forventet = ForventetRespons.json(500, """{"melding":"Noe gikk galt på serversiden","kode":"server_feil"}"""),
            )
        }
        // PDL-feil stopper flyten før tiltakshistorikk kalles.
        kontekst.tiltakshistorikkTransport.mottatteKall.size shouldBe 0
    }
}
