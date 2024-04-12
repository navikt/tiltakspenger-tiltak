package no.nav.tiltakspenger.tiltak.services

import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

internal class KometServiceTest {

    private val testRapid = TestRapid()

    init {
        KometService(
            rapidsConnection = testRapid,
        )
    }

    @AfterEach
    fun reset() {
        testRapid.reset()
    }

    @Test
    fun `sjekk at vi kan lese en melding fra Komet`() {
        testRapid.sendTestMessage(gyldigJson)
    }

    @Test
    fun `sjekk at vi kan lese en melding med null felter fra Komet`() {
        testRapid.sendTestMessage(jsonMedNullFelter)
        // with(testRapid.inspektør) {
        // println("Dette er meldingen vi sender ${this.message(0)}")
        // size shouldBe 1
        // }
    }

    private val gyldigJson = """
        {
          "id": "bd3b6087-2029-481b-bcf0-e37354c00286",
          "gjennomforingId": "1487f7fe-156c-41d7-8d90-bf108dd1b4d2",
          "personIdent": "12345678942",
          "startDato": "2022-02-25",
          "sluttDato": "2022-05-20",
          "status": {
            "type": "HAR_SLUTTET",
            "aarsak": "FATT_JOBB",
            "opprettetDato": "2023-10-24T11:47:48.254204"
          },
          "registrertDato": "2022-01-27T16:13:39",
          "dagerPerUke": 3,
          "prosentStilling": 50,
          "endretDato": "2023-10-24T11:47:48.254204",
          "kilde": "ARENA"
        }
    """.trimIndent()

    private val jsonMedNullFelter = """
        {
          "id": "bd3b6087-2029-481b-bcf0-e37354c00286",
          "gjennomforingId": "1487f7fe-156c-41d7-8d90-bf108dd1b4d2",
          "personIdent": "12345678942",
          "startDato": null,
          "sluttDato": null,
          "status": {
            "type": "HAR_SLUTTET",
            "aarsak": null,
            "opprettetDato": "2023-10-24T11:47:48.254204"
          },
          "registrertDato": "2022-01-27T16:13:39",
          "dagerPerUke": null,
          "prosentStilling": null,
          "endretDato": "2023-10-24T11:47:48.254204",
          "kilde": null
        }
    """.trimIndent()
}
