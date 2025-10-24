package no.nav.tiltakspenger.tiltak.gjennomforing.tiltakstype.db

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.tiltak.TiltakResponsDTO
import no.nav.tiltakspenger.tiltak.gjennomforing.tiltakstype.kafka.TiltakstypeDto
import no.nav.tiltakspenger.tiltak.testutils.withMigratedDb
import org.junit.jupiter.api.Test
import java.util.UUID

class TiltakstypeRepoTest {
    @Test
    fun `kan lagre og hente tiltakstype`() {
        withMigratedDb { testDataHelper ->
            val tiltakstypeRepo = testDataHelper.tiltakstypeRepo

            val tiltakstypeDto = TiltakstypeDto(
                id = UUID.randomUUID(),
                navn = "Jobbklubb",
                tiltakskode = Tiltakskode.JOBBKLUBB,
                arenaKode = "JOBBK",
            )

            tiltakstypeRepo.lagre(tiltakstypeDto.toTiltakstype())

            val tiltakstypeFraDb = tiltakstypeRepo.hent(tiltakstypeDto.id)

            tiltakstypeFraDb shouldBe Tiltakstype(
                id = tiltakstypeDto.id,
                navn = tiltakstypeDto.navn,
                tiltakskode = Tiltakskode.JOBBKLUBB,
                arenakode = TiltakResponsDTO.TiltakType.JOBBK,
            )
        }
    }
}
