package no.nav.tiltakspenger.tiltak.clients.valp

import io.kotest.matchers.shouldBe
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

internal class ValpClientImplTest {

    companion object {
        const val accessToken = "woopwoop"
    }

    @Test
    fun `lese en ok melding fra Valp`() {
        val tiltakId = "1"
        val mockEngine = MockEngine { request ->
            request.headers["Authorization"]
            when (request.url.toString()) {
                "http://localhost/api/v1/tiltaksgjennomforinger/$tiltakId" -> respond(
                    content = successJson,
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )

                else -> throw RuntimeException("Should not happen")
            }
        }

        val client = ValpClientImpl(
            getToken = { accessToken },
            engine = mockEngine,
        )

        runBlocking {
            val gjennomføring = client.hentTiltakGjennomføring(tiltakId)

            gjennomføring shouldBe ValpDTO(
                id = UUID.fromString("825ed2fa-6506-4fe1-a6c1-2120cf6c9abb"),
                tiltakstype = Tiltakstype(
                    id = UUID.fromString("9b52265c-914c-413d-bca4-e9d7b3f1bd8d"),
                    navn = "Gruppe AMO",
                    arenaKode = "GRUPPEAMO",
                ),
                navn = "Lars - gruppe-AMO test",
                startDato = LocalDate.of(2023, 2, 25),
                sluttDato = LocalDate.of(2023, 3, 25),
                status = Tiltaksgjennomforingsstatus.AVSLUTTET,
                virksomhetsnummer = "974548283",
                oppstart = TiltaksgjennomforingOppstartstype.FELLES,
            )
        }
    }

    private val successJson = """
        {
          "id": "825ed2fa-6506-4fe1-a6c1-2120cf6c9abb",
          "tiltakstype": {
            "id": "9b52265c-914c-413d-bca4-e9d7b3f1bd8d",
            "navn": "Gruppe AMO",
            "arenaKode": "GRUPPEAMO"
          },
          "navn": "Lars - gruppe-AMO test",
          "startDato": "2023-02-25",
          "sluttDato": "2023-03-25",
          "status": "AVSLUTTET",
          "virksomhetsnummer": "974548283",
          "oppstart": "FELLES"
        }
    """.trimIndent()
}
