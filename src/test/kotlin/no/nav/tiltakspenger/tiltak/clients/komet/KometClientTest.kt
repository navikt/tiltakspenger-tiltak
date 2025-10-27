package no.nav.tiltakspenger.tiltak.clients.komet

import io.kotest.matchers.collections.shouldContainAll
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import no.nav.tiltakspenger.libs.common.AccessToken
import no.nav.tiltakspenger.libs.tiltak.KometDeltakerStatusType
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

internal class KometClientTest {

    companion object {
        val ACCESS_TOKEN = AccessToken(
            token = "woopwoop",
            expiresAt = java.time.Instant.now().plusSeconds(5000L),
            invaliderCache = {},
        )
    }

    @Test
    fun `lese en ok melding fra Komet`() {
        val mockEngine = MockEngine { request ->
            request.headers["Authorization"]
            when (request.url.toString()) {
                "http://localhost/external/deltakelser" -> respond(
                    content = successJson,
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )

                else -> throw RuntimeException("Should not happen")
            }
        }

        val client = KometClient(
            baseUrl = "http://localhost",
            getToken = { ACCESS_TOKEN },
            engine = mockEngine,
        )

        runBlocking {
            val deltakere = client.hentTiltakDeltagelser("123", "correlationId")

            deltakere shouldContainAll listOf(
                KometResponseJson(
                    id = "1c51c943-ce2d-4029-8c1e-18b3c59d3e2e",
                    startDato = null,
                    sluttDato = null,
                    status = KometDeltakerStatusType.IKKE_AKTUELL,
                    dagerPerUke = 2.0F,
                    prosentStilling = 100.0F,
                    registrertDato = LocalDateTime.of(2022, 2, 17, 14, 53, 31),
                    gjennomforing = KometResponseJson.GjennomforingDTO(
                        id = "bc4a05a5-56ed-47ac-8176-b685b0731751",
                        navn = "Testing Linn 1",
                        type = "INDOPPFAG",
                        tiltakstypeNavn = "Oppfølging",
                        arrangor = KometResponseJson.GjennomforingDTO.ArrangorDTO(
                            virksomhetsnummer = "974548283",
                            navn = "TINN KOMMUNE KOMMUNEDIREKTØRENS STAB",
                        ),
                    ),
                ),
            )
        }
    }

    private val successJson = """
        [
        {
            "id": "1c51c943-ce2d-4029-8c1e-18b3c59d3e2e",
            "gjennomforing": {
              "id": "bc4a05a5-56ed-47ac-8176-b685b0731751",
              "navn": "Testing Linn 1",
              "tiltakstypeNavn": "Oppfølging",
              "type": "INDOPPFAG",
              "arrangor": {
                "virksomhetsnummer": "974548283",
                "navn": "TINN KOMMUNE KOMMUNEDIREKTØRENS STAB"
              }
            },
            "startDato": null,
            "sluttDato": null,
            "status": "IKKE_AKTUELL",
            "dagerPerUke": 2.0,
            "prosentStilling": 100.0,
            "registrertDato": "2022-02-17T14:53:31"
          },
          {
            "id": "a539a93b-06d5-42db-9598-bbeda5b18ee5",
            "gjennomforing": {
              "id": "e0036a1f-ba82-4a45-95d0-8d5008d8881c",
              "navn": "Astronomisk testtiltak - oppfølging",
              "tiltakstypeNavn": "Oppfølging",
              "type": "INDOPPFAG",
              "arrangor": {
                "virksomhetsnummer": "974750449",
                "navn": "STATSFORVALTEREN I TRØNDELAG TRONDHEIM"
              }
            },
            "startDato": null,
            "sluttDato": null,
            "status": "IKKE_AKTUELL",
            "dagerPerUke": null,
            "prosentStilling": 100.0,
            "registrertDato": "2022-08-01T15:09:18"
          },
          {
            "id": "e38d663d-c78c-4eb8-8a9a-8b9bade0c61a",
            "gjennomforing": {
              "id": "e700a519-b41a-4407-9dac-6d2375cd4c50",
              "navn": "oppfølgingstiltak linn,alex,lars",
              "tiltakstypeNavn": "Oppfølging",
              "type": "INDOPPFAG",
              "arrangor": {
                "virksomhetsnummer": "974750449",
                "navn": "STATSFORVALTEREN I TRØNDELAG TRONDHEIM"
              }
            },
            "startDato": "2022-01-19",
            "sluttDato": "2022-02-18",
            "status": "HAR_SLUTTET",
            "dagerPerUke": 5.0,
            "prosentStilling": 100.0,
            "registrertDato": "2022-02-15T11:11:03"
          },
          {
            "id": "330a14f3-be15-4b86-b017-145c6db43910",
            "gjennomforing": {
              "id": "c0656b88-39a6-4d96-b3e0-6271ecb48698",
              "navn": "testtiltak oppfølging 20.01.2022",
              "tiltakstypeNavn": "Oppfølging",
              "type": "INDOPPFAG",
              "arrangor": {
                "virksomhetsnummer": "974750449",
                "navn": "STATSFORVALTEREN I TRØNDELAG TRONDHEIM"
              }
            },
            "startDato": "2022-01-21",
            "sluttDato": "2022-02-05",
            "status": "HAR_SLUTTET",
            "dagerPerUke": 5.0,
            "prosentStilling": 100.0,
            "registrertDato": "2022-02-04T15:05:34"
          },
          {
            "id": "7314a163-2441-439c-a176-1d736222f37d",
            "gjennomforing": {
              "id": "eae379be-306b-4f09-b473-f01483975eb4",
              "navn": "Izis testgjennomføring",
              "tiltakstypeNavn": "Oppfølging",
              "type": "INDOPPFAG",
              "arrangor": {
                "virksomhetsnummer": "974750449",
                "navn": "STATSFORVALTEREN I TRØNDELAG TRONDHEIM"
              }
            },
            "startDato": null,
            "sluttDato": "2023-03-01",
            "status": "IKKE_AKTUELL",
            "dagerPerUke": null,
            "prosentStilling": 100.0,
            "registrertDato": "2022-06-28T09:50:37"
          },
          {
            "id": "b1213f00-cd3f-471d-9144-db80fdb177f3",
            "gjennomforing": {
              "id": "c4212ab6-1b48-4ff7-bb4f-bbc086e6a2d2",
              "navn": "Avklaring teste at ikke dukker opp hos Komet",
              "tiltakstypeNavn": "Oppfølging",
              "type": "AVKLARAG",
              "arrangor": {
                "virksomhetsnummer": "974750449",
                "navn": "STATSFORVALTEREN I TRØNDELAG TRONDHEIM"
              }
            },
            "startDato": "2022-02-15",
            "sluttDato": "2022-03-15",
            "status": "HAR_SLUTTET",
            "dagerPerUke": 5.0,
            "prosentStilling": 100.0,
            "registrertDato": "2022-02-15T15:43:03"
          },
          {
            "id": "11e8e2de-b03e-4c6f-804d-9718045f66f3",
            "gjennomforing": {
              "id": "f34644e1-3216-4251-85fc-6114ff0ab420",
              "navn": "test AFT Tinn bhg - Lars",
              "tiltakstypeNavn": "Oppfølging",
              "type": "ARBFORB",
              "arrangor": {
                "virksomhetsnummer": "914016444",
                "navn": "TINN KOMMUNE ENHET FOR SKOLE OG BARNEHAGE"
              }
            },
            "startDato": "2023-05-07",
            "sluttDato": "2023-07-02",
            "status": "IKKE_AKTUELL",
            "dagerPerUke": null,
            "prosentStilling": 100.0,
            "registrertDato": "2023-03-10T09:56:26"
          },
          {
            "id": "20f55d7a-f598-4296-b617-41360cdce6f4",
            "gjennomforing": {
              "id": "c3e72527-5447-48f1-b51d-2ddea382850e",
              "navn": "oppfølging Tinn avd. 1 - Lars",
              "tiltakstypeNavn": "Oppfølging",
              "type": "INDOPPFAG",
              "arrangor": {
                "virksomhetsnummer": "974548283",
                "navn": "TINN KOMMUNE KOMMUNEDIREKTØRENS STAB"
              }
            },
            "startDato": null,
            "sluttDato": null,
            "status": "VENTER_PA_OPPSTART",
            "dagerPerUke": null,
            "prosentStilling": 100.0,
            "registrertDato": "2023-03-13T08:11:07"
          },
          {
            "id": "b562cf12-3d09-498b-b3af-37041de9853a",
            "gjennomforing": {
              "id": "8c42c8cd-f832-41fe-b2a7-67eda83c1705",
              "navn": "ARR - Tinn harnehage - Lars",
              "tiltakstypeNavn": "Arbeidsrettet rehabilitering (dag)",
              "type": "ARBRRHDAG",
              "arrangor": {
                "virksomhetsnummer": "914016444",
                "navn": "TINN KOMMUNE ENHET FOR SKOLE OG BARNEHAGE"
              }
            },
            "startDato": "2023-03-15",
            "sluttDato": "2023-04-15",
            "status": "HAR_SLUTTET",
            "dagerPerUke": null,
            "prosentStilling": 100.0,
            "registrertDato": "2023-03-13T08:25:18"
          },
          {
            "id": "bb46b2f7-4835-4943-b294-4677e3d8fd6c",
            "gjennomforing": {
              "id": "ed7a3161-495c-4409-b08e-128a6f0d03ce",
              "navn": "Oppfølging Tinn bhg - test Lars",
              "tiltakstypeNavn": "Oppfølging",
              "type": "INDOPPFAG",
              "arrangor": {
                "virksomhetsnummer": "914016444",
                "navn": "TINN KOMMUNE ENHET FOR SKOLE OG BARNEHAGE"
              }
            },
            "startDato": null,
            "sluttDato": null,
            "status": "VENTER_PA_OPPSTART",
            "dagerPerUke": null,
            "prosentStilling": 100.0,
            "registrertDato": "2023-03-10T08:57:02"
          },
          {
            "id": "69d0841d-cbdb-4d98-9721-bd9f81eefd53",
            "gjennomforing": {
              "id": "a972d2c1-7fb4-4207-af5d-4fea5561ea60",
              "navn": "Modist vg 2 - Lars fra Mars",
              "tiltakstypeNavn": "Gruppe AMO",
              "type": "GRUPPEAMO",
              "arrangor": {
                "virksomhetsnummer": "974548283",
                "navn": "TINN KOMMUNE KOMMUNEDIREKTØRENS STAB"
              }
            },
            "startDato": null,
            "sluttDato": "2024-04-27",
            "status": "VENTER_PA_OPPSTART",
            "dagerPerUke": null,
            "prosentStilling": 100.0,
            "registrertDato": "2023-04-25T12:59:11"
          },
          {
            "id": "9558710b-c90c-40d2-899f-f1e3084627e0",
            "gjennomforing": {
              "id": "cff08d1e-62f7-4f85-9faf-aef8e21c4021",
              "navn": "Fagkurs - Astronomisk hattemakeri",
              "tiltakstypeNavn": "Gruppe Fag- og yrkesopplæring VGS og høyere yrkesfaglig utdanning",
              "type": "GRUFAGYRKE",
              "arrangor": {
                "virksomhetsnummer": "974750449",
                "navn": "STATSFORVALTEREN I TRØNDELAG TRONDHEIM"
              }
            },
            "startDato": "2023-04-26",
            "sluttDato": "2023-04-30",
            "status": "IKKE_AKTUELL",
            "dagerPerUke": null,
            "prosentStilling": 100.0,
            "registrertDato": "2023-04-21T07:38:29"
          },
          {
            "id": "bb5a194f-5274-41ea-a46a-e86b5155151a",
            "gjennomforing": {
              "id": "825ed2fa-6506-4fe1-a6c1-2120cf6c9abb",
              "navn": "Lars - gruppe-AMO test",
              "tiltakstypeNavn": "Gruppe AMO",
              "type": "GRUPPEAMO",
              "arrangor": {
                "virksomhetsnummer": "974548283",
                "navn": "TINN KOMMUNE KOMMUNEDIREKTØRENS STAB"
              }
            },
            "startDato": null,
            "sluttDato": null,
            "status": "IKKE_AKTUELL",
            "dagerPerUke": null,
            "prosentStilling": 100.0,
            "registrertDato": "2023-02-28T08:36:18"
          }
          ]
    """.trimIndent()
}
