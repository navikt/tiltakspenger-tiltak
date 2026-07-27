package no.nav.tiltakspenger.tiltak.clients.tiltakshistorikk

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.infra.transport.FakeHttpTransport
import no.nav.tiltakspenger.tiltak.clients.tiltakshistorikk.dto.ArenaDeltakerStatusDto
import no.nav.tiltakspenger.tiltak.clients.tiltakshistorikk.dto.TiltakshistorikkV1Dto
import no.nav.tiltakspenger.tiltak.clients.tiltakshistorikk.dto.TiltakshistorikkV1Response
import no.nav.tiltakspenger.tiltak.testutils.testTokenProvider
import org.junit.jupiter.api.Test
import java.io.IOException
import java.net.http.HttpTimeoutException
import java.time.LocalDate
import java.util.UUID

internal class TiltakshistorikkClientTest {
    private val baseUrl = "http://tiltakshistorikk.test"
    private val fnr = "12345678910"

    private fun klient(transport: FakeHttpTransport) = TiltakshistorikkClient(
        baseUrl = baseUrl,
        clock = fixedClock,
        authTokenProvider = testTokenProvider,
        transport = transport,
    )

    private fun arenaDeltakelse(id: UUID = UUID.randomUUID()) = TiltakshistorikkV1Dto.ArenaDeltakelse(
        startDato = LocalDate.of(2024, 1, 1),
        sluttDato = LocalDate.of(2024, 3, 31),
        id = id,
        tittel = "Tiltak hos arrangør",
        arenaId = 1234567,
        status = ArenaDeltakerStatusDto.GJENNOMFORES,
        tiltakstype = TiltakshistorikkV1Dto.ArenaDeltakelse.Tiltakstype(
            tiltakskode = "ENKELAMO",
            navn = "Arbeidsmarkedsopplæring (enkeltplass)",
        ),
        gjennomforing = TiltakshistorikkV1Dto.Gjennomforing(id = UUID.randomUUID(), deltidsprosent = 100.0f),
        arrangor = TiltakshistorikkV1Dto.Arrangor(
            hovedenhet = null,
            underenhet = TiltakshistorikkV1Dto.Virksomhet(navn = "Arrangør"),
        ),
        deltidsprosent = 100.0f,
        dagerPerUke = 5.0f,
    )

    @Test
    fun `bygger default HttpKlient-oppsett når transport ikke sendes inn`() {
        TiltakshistorikkClient(
            baseUrl = baseUrl,
            clock = fixedClock,
            authTokenProvider = testTokenProvider,
        )
    }

    @Test
    fun `henter tiltaksdeltakelser og sender identene i bodyen`() = runTest {
        val deltakelse = arenaDeltakelse()
        val transport = FakeHttpTransport().apply { leggIKøJson(TiltakshistorikkV1Response(historikk = listOf(deltakelse))) }

        val deltakelser = klient(transport).hentTiltaksdeltakelser(listOf(fnr)).getOrFail()

        deltakelser.map { it.id } shouldBe listOf(deltakelse.id)
        val kall = transport.mottatteKall.single()
        kall.metode shouldBe "POST"
        kall.uri.toString() shouldBe "$baseUrl/api/v1/historikk"
        kall.bodyTekst shouldContain fnr
        kall.request.headers().firstValue("Authorization").get() shouldBe "Bearer token"
    }

    @Test
    fun `tomt svar gir tom liste`() = runTest {
        val transport = FakeHttpTransport().apply { leggIKøJson(TiltakshistorikkV1Response(historikk = emptyList())) }

        klient(transport).hentTiltaksdeltakelser(listOf(fnr)).getOrFail() shouldBe emptyList()
    }

    @Test
    fun `andre 2xx enn 200 godtas ikke`() = runTest {
        val transport = FakeHttpTransport().apply {
            leggIKøJson(TiltakshistorikkV1Response(historikk = emptyList()), statusCode = 202)
        }

        val feil = klient(transport).hentTiltaksdeltakelser(listOf(fnr)).leftOrNull().shouldNotBeNull()

        feil.shouldBeInstanceOf<HttpKlientError.UventetStatus>().statusCode shouldBe 202
        // 202 er ikke en retryable status, så det gjøres bare ett forsøk.
        transport.mottatteKall.size shouldBe 1
    }

    @Test
    fun `serverfeil retryes tre ganger før den gir Left`() = runTest {
        val transport = FakeHttpTransport().apply { leggIKøStatusForAlleForsøk(statusCode = 500, body = "kaboom", maksForsøk = 3) }

        val feil = klient(transport).hentTiltaksdeltakelser(listOf(fnr)).leftOrNull().shouldNotBeNull()

        feil.shouldBeInstanceOf<HttpKlientError.UventetStatus>().statusCode shouldBe 500
        // Retry-budsjettet er tre forsøk fordi 3 × 7 s + backoff må få plass i tidsbudsjettet klienten deler med PDL-oppslaget.
        transport.mottatteKall.size shouldBe 3
    }

    @Test
    fun `timeout retryes og gir Left`() = runTest {
        val transport = FakeHttpTransport().apply {
            leggIKøKastForAlleForsøk(HttpTimeoutException("simulert timeout"), maksForsøk = 3)
        }

        val feil = klient(transport).hentTiltaksdeltakelser(listOf(fnr)).leftOrNull().shouldNotBeNull()

        feil.shouldBeInstanceOf<HttpKlientError.Timeout>()
        transport.mottatteKall.size shouldBe 3
    }

    @Test
    fun `nettverksfeil gir Left`() = runTest {
        val transport = FakeHttpTransport().apply {
            leggIKøKastForAlleForsøk(IOException("simulert nettverksfeil"), maksForsøk = 3)
        }

        val feil = klient(transport).hentTiltaksdeltakelser(listOf(fnr)).leftOrNull().shouldNotBeNull()

        feil.shouldBeInstanceOf<HttpKlientError.NetworkError>()
    }

    @Test
    fun `klientfeil retryes ikke`() = runTest {
        val transport = FakeHttpTransport().apply { leggIKøStatus(statusCode = 400, body = "nei") }

        val feil = klient(transport).hentTiltaksdeltakelser(listOf(fnr)).leftOrNull().shouldNotBeNull()

        feil.shouldBeInstanceOf<HttpKlientError.UventetStatus>().statusCode shouldBe 400
        transport.mottatteKall.size shouldBe 1
    }
}
