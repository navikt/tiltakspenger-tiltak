package no.nav.tiltakspenger.tiltak.person.infra.http.pdl

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.infra.transport.FakeHttpTransport
import no.nav.tiltakspenger.tiltak.testutils.testTokenProvider
import org.junit.jupiter.api.Test
import java.io.IOException

internal class PdlClientTest {
    private val baseUrl = "http://pdl.test"
    private val fnr = "12345678910"
    private val historiskFnr = "10987654321"

    private fun klient(transport: FakeHttpTransport) = PdlClient(
        baseUrl = baseUrl,
        clock = fixedClock,
        authTokenProvider = testTokenProvider,
        transport = transport,
    )

    //language=JSON
    private val happyJson = """
        {
          "data": {
            "hentIdenter": {
              "identer": [
                { "ident": "$fnr" },
                { "ident": "$historiskFnr" }
              ]
            }
          },
          "errors": null
        }
    """.trimIndent()

    @Test
    fun `bygger default HttpKlient-oppsett når transport ikke sendes inn`() {
        PdlClient(
            baseUrl = baseUrl,
            clock = fixedClock,
            authTokenProvider = testTokenProvider,
        )
    }

    @Test
    fun `henter nåværende og historiske identer og sender PDL-headerne`() = runTest {
        val transport = FakeHttpTransport().apply { leggIKøJson(happyJson) }

        val identer = klient(transport).hentNåværendeOgHistoriskeFødselsnummer(fnr).getOrFail()

        identer shouldBe listOf(fnr, historiskFnr)
        val kall = transport.mottatteKall.single()
        kall.metode shouldBe "POST"
        kall.uri.toString() shouldBe "$baseUrl/graphql"
        kall.request.headers().firstValue("Tema").get() shouldBe "IND"
        kall.request.headers().firstValue("behandlingsnummer").get() shouldBe "B470"
        kall.request.headers().firstValue("Authorization").get() shouldBe "Bearer token"
        kall.bodyTekst shouldContain fnr
        kall.bodyTekst shouldContain "hentIdenter"
    }

    @Test
    fun `godtar alle 2xx`() = runTest {
        val transport = FakeHttpTransport().apply { leggIKøJson(happyJson, statusCode = 201) }

        klient(transport).hentNåværendeOgHistoriskeFødselsnummer(fnr).getOrFail() shouldBe listOf(fnr, historiskFnr)
    }

    @Test
    fun `graphql-feil gir GraphQLFeil med feilmeldingene`() = runTest {
        val transport = FakeHttpTransport().apply {
            leggIKøJson(
                """{"data": {"hentIdenter": null}, "errors": [{"message": "Fant ikke person", "locations": null, "path": null, "extensions": {"code": "not_found", "classification": null}}]}""",
            )
        }

        val feil = klient(transport).hentNåværendeOgHistoriskeFødselsnummer(fnr).leftOrNull().shouldNotBeNull()

        feil.shouldBeInstanceOf<KanIkkeHentePerson.GraphQLFeil>().feilmeldinger shouldBe listOf("Fant ikke person")
    }

    @Test
    fun `graphql-feil uten melding rapporteres som ukjent`() = runTest {
        val transport = FakeHttpTransport().apply {
            leggIKøJson("""{"data": null, "errors": [{"message": null, "locations": null, "path": null, "extensions": null}]}""")
        }

        val feil = klient(transport).hentNåværendeOgHistoriskeFødselsnummer(fnr).leftOrNull().shouldNotBeNull()

        feil.shouldBeInstanceOf<KanIkkeHentePerson.GraphQLFeil>().feilmeldinger shouldBe listOf("ukjent")
    }

    @Test
    fun `tom identliste gir FantIngenIdenter`() = runTest {
        val transport = FakeHttpTransport().apply {
            leggIKøJson("""{"data": {"hentIdenter": {"identer": []}}, "errors": null}""")
        }

        klient(transport).hentNåværendeOgHistoriskeFødselsnummer(fnr)
            .leftOrNull()
            .shouldNotBeNull()
            .shouldBeInstanceOf<KanIkkeHentePerson.FantIngenIdenter>()
    }

    @Test
    fun `respons uten data gir FantIngenIdenter`() = runTest {
        val transport = FakeHttpTransport().apply { leggIKøJson("""{"data": null, "errors": null}""") }

        klient(transport).hentNåværendeOgHistoriskeFødselsnummer(fnr)
            .leftOrNull()
            .shouldNotBeNull()
            .shouldBeInstanceOf<KanIkkeHentePerson.FantIngenIdenter>()
    }

    @Test
    fun `uventet status gir KallFeilet uten retry`() = runTest {
        val transport = FakeHttpTransport().apply { leggIKøStatus(statusCode = 500, body = "kaboom") }

        val feil = klient(transport).hentNåværendeOgHistoriskeFødselsnummer(fnr).leftOrNull().shouldNotBeNull()

        feil.shouldBeInstanceOf<KanIkkeHentePerson.KallFeilet>()
            .httpKlientError
            .shouldBeInstanceOf<HttpKlientError.UventetStatus>()
            .statusCode shouldBe 500
        // Klienten har Retry.Ingen, som den gamle ktor-klienten.
        transport.mottatteKall.size shouldBe 1
    }

    @Test
    fun `nettverksfeil gir KallFeilet`() = runTest {
        val transport = FakeHttpTransport().apply { leggIKøKast(IOException("simulert nettverksfeil")) }

        val feil = klient(transport).hentNåværendeOgHistoriskeFødselsnummer(fnr).leftOrNull().shouldNotBeNull()

        feil.shouldBeInstanceOf<KanIkkeHentePerson.KallFeilet>()
            .httpKlientError
            .shouldBeInstanceOf<HttpKlientError.NetworkError>()
    }

    @Test
    fun `identen maskeres i toString på requesten`() {
        PdlVariables(ident = fnr).toString() shouldBe "PdlVariables(ident=*****)"
    }
}
