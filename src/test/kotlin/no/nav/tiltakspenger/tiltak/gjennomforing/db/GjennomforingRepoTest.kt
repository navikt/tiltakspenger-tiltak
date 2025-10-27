package no.nav.tiltakspenger.tiltak.gjennomforing.db

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.tiltakspenger.libs.tiltak.TiltakResponsDTO
import no.nav.tiltakspenger.tiltak.gjennomforing.kafka.TiltaksgjennomforingV1Dto
import no.nav.tiltakspenger.tiltak.gjennomforing.tiltakstype.db.Tiltakskode
import no.nav.tiltakspenger.tiltak.gjennomforing.tiltakstype.db.Tiltakstype
import no.nav.tiltakspenger.tiltak.testutils.withMigratedDb
import org.junit.jupiter.api.Test
import java.util.UUID

class GjennomforingRepoTest {
    @Test
    fun `kan lagre og hente gjennomføring`() {
        withMigratedDb { testDataHelper ->
            val gjennomforingRepo = testDataHelper.gjennomforingRepo
            val tiltakstypeRepo = testDataHelper.tiltakstypeRepo

            val tiltakstype = Tiltakstype(
                id = UUID.randomUUID(),
                navn = "Jobbklubb",
                tiltakskode = Tiltakskode.JOBBKLUBB,
                arenakode = TiltakResponsDTO.TiltakType.JOBBK,
            )
            tiltakstypeRepo.lagre(tiltakstype)

            val tiltaksgjennomforingV1Dto = TiltaksgjennomforingV1Dto(
                id = UUID.randomUUID(),
                tiltakstype = TiltaksgjennomforingV1Dto.Tiltakstype(
                    id = tiltakstype.id,
                ),
                deltidsprosent = 100.0,
            )

            gjennomforingRepo.lagre(tiltaksgjennomforingV1Dto.toGjennomforing())

            val gjennomforingFraDb = gjennomforingRepo.hent(tiltaksgjennomforingV1Dto.id)

            gjennomforingFraDb shouldBe Gjennomforing(
                id = tiltaksgjennomforingV1Dto.id,
                tiltakstypeId = tiltaksgjennomforingV1Dto.tiltakstype.id,
                deltidsprosent = tiltaksgjennomforingV1Dto.deltidsprosent,
            )
        }
    }

    @Test
    fun `kan slette gjennomføring`() {
        withMigratedDb { testDataHelper ->
            val gjennomforingRepo = testDataHelper.gjennomforingRepo
            val tiltakstypeRepo = testDataHelper.tiltakstypeRepo

            val tiltakstype = Tiltakstype(
                id = UUID.randomUUID(),
                navn = "Jobbklubb",
                tiltakskode = Tiltakskode.JOBBKLUBB,
                arenakode = TiltakResponsDTO.TiltakType.JOBBK,
            )
            tiltakstypeRepo.lagre(tiltakstype)

            val gjennomforing = Gjennomforing(
                id = UUID.randomUUID(),
                tiltakstypeId = tiltakstype.id,
                deltidsprosent = 100.0,
            )
            gjennomforingRepo.lagre(gjennomforing)
            gjennomforingRepo.hent(gjennomforing.id) shouldNotBe null

            gjennomforingRepo.slett(gjennomforing.id)

            gjennomforingRepo.hent(gjennomforing.id) shouldBe null
        }
    }
}
