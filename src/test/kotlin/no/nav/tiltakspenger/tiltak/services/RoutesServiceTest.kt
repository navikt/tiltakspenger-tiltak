package no.nav.tiltakspenger.tiltak.services

import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import no.nav.tiltakspenger.libs.arena.tiltak.ArenaDeltakerStatusType
import no.nav.tiltakspenger.libs.arena.tiltak.ArenaTiltaksaktivitetResponsDTO
import no.nav.tiltakspenger.libs.arena.tiltak.ArenaTiltaksaktivitetResponsDTO.TiltakType
import no.nav.tiltakspenger.libs.arena.tiltak.ArenaTiltaksaktivitetResponsDTO.TiltakType.AMO
import no.nav.tiltakspenger.libs.arena.tiltak.ArenaTiltaksaktivitetResponsDTO.TiltakType.AMOB
import no.nav.tiltakspenger.libs.arena.tiltak.ArenaTiltaksaktivitetResponsDTO.TiltakType.AMOE
import no.nav.tiltakspenger.libs.arena.tiltak.ArenaTiltaksaktivitetResponsDTO.TiltakType.AMOY
import no.nav.tiltakspenger.libs.arena.tiltak.ArenaTiltaksaktivitetResponsDTO.TiltakType.ARBTREN
import no.nav.tiltakspenger.libs.arena.tiltak.ArenaTiltaksaktivitetResponsDTO.TiltakType.HOYEREUTD
import no.nav.tiltakspenger.libs.arena.tiltak.ArenaTiltaksaktivitetResponsDTO.TiltakType.KURS
import no.nav.tiltakspenger.libs.tiltak.KometDeltakerStatusType
import no.nav.tiltakspenger.libs.tiltak.TiltakResponsDTO
import no.nav.tiltakspenger.libs.tiltak.TiltakResponsDTO.DeltakerStatusDTO.DELTAR
import no.nav.tiltakspenger.libs.tiltak.TiltakResponsDTO.DeltakerStatusDTO.FEILREGISTRERT
import no.nav.tiltakspenger.libs.tiltak.TiltakResponsDTO.DeltakerStatusDTO.IKKE_AKTUELL
import no.nav.tiltakspenger.libs.tiltak.TiltakResponsDTO.DeltakerStatusDTO.PABEGYNT_REGISTRERING
import no.nav.tiltakspenger.libs.tiltak.TiltakResponsDTO.DeltakerStatusDTO.SOKT_INN
import no.nav.tiltakspenger.libs.tiltak.TiltakResponsDTO.DeltakerStatusDTO.VENTELISTE
import no.nav.tiltakspenger.libs.tiltak.TiltakResponsDTO.DeltakerStatusDTO.VENTER_PA_OPPSTART
import no.nav.tiltakspenger.libs.tiltak.TiltakResponsDTO.DeltakerStatusDTO.VURDERES
import no.nav.tiltakspenger.tiltak.clients.arena.ArenaClient
import no.nav.tiltakspenger.tiltak.clients.komet.KometClient
import no.nav.tiltakspenger.tiltak.clients.komet.KometResponseJson
import no.nav.tiltakspenger.tiltak.gjennomforing.db.Gjennomforing
import no.nav.tiltakspenger.tiltak.gjennomforing.db.GjennomforingRepo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

internal class RoutesServiceTest {
    private val kometClient = mockk<KometClient>()
    private val arenaClient = mockk<ArenaClient>()
    private val gjennomforingRepo = mockk<GjennomforingRepo>()

    @BeforeEach
    fun setup() {
        every { gjennomforingRepo.hent(any()) } returns Gjennomforing(
            id = UUID.randomUUID(),
            tiltakstypeId = UUID.randomUUID(),
            deltidsprosent = 100.0,
        )
    }

    @Test
    fun `tiltak som ikke gir rett til Tiltakspenger er ikke med i søknaden selv om de har riktig status`() {
        val routesService = RoutesService(
            kometClient = kometClient,
            arenaClient = arenaClient,
            gjennomforingRepo = gjennomforingRepo,

        )

        coEvery { kometClient.hentTiltakDeltagelser(any(), any()) } returns listOf(
            kometDeltaker("VASV", KometDeltakerStatusType.AVBRUTT),
        )
        coEvery { arenaClient.hentTiltakArena(any(), any()) } returns listOf(
            arenaTiltak(KURS, ArenaDeltakerStatusType.DELAVB),
        )

        val tiltakListe = routesService.hentTiltakForSøknad("123", "correlationId")

        tiltakListe.size shouldBe 0
    }

    @Test
    fun `tiltak fra komet og arena som ikke gir rett til å søke er ikke med i listen`() {
        val routesService = RoutesService(
            kometClient = kometClient,
            arenaClient = arenaClient,
            gjennomforingRepo = gjennomforingRepo,
        )

        coEvery { kometClient.hentTiltakDeltagelser(any(), any()) } returns listOf(
            // Disse skal være med...
            kometDeltaker("ARBFORB", KometDeltakerStatusType.AVBRUTT),
            kometDeltaker("ARBFORB", KometDeltakerStatusType.FULLFORT),
            kometDeltaker("ARBFORB", KometDeltakerStatusType.DELTAR),
            kometDeltaker("ARBFORB", KometDeltakerStatusType.VENTER_PA_OPPSTART),
            kometDeltaker("ARBFORB", KometDeltakerStatusType.HAR_SLUTTET),
            // Disse skal ikke være med
            kometDeltaker("ARBFORB", KometDeltakerStatusType.IKKE_AKTUELL),
            kometDeltaker("ARBFORB", KometDeltakerStatusType.VURDERES),
            kometDeltaker("ARBFORB", KometDeltakerStatusType.FEILREGISTRERT),
            kometDeltaker("ARBFORB", KometDeltakerStatusType.PABEGYNT_REGISTRERING),
            kometDeltaker("ARBFORB", KometDeltakerStatusType.SOKT_INN),
            kometDeltaker("ARBFORB", KometDeltakerStatusType.VENTELISTE),
        )
        coEvery { arenaClient.hentTiltakArena(any(), any()) } returns listOf(
            // Disse skal være med
            arenaTiltak(ARBTREN, ArenaDeltakerStatusType.DELAVB),
            arenaTiltak(ARBTREN, ArenaDeltakerStatusType.FULLF),
            arenaTiltak(ARBTREN, ArenaDeltakerStatusType.GJENN),
            arenaTiltak(ARBTREN, ArenaDeltakerStatusType.GJENN_AVB),
            arenaTiltak(ARBTREN, ArenaDeltakerStatusType.IKKEM),
            arenaTiltak(ARBTREN, ArenaDeltakerStatusType.JATAKK),
            arenaTiltak(ARBTREN, ArenaDeltakerStatusType.TILBUD),

            // Disse skal ikke være med
            arenaTiltak(ARBTREN, ArenaDeltakerStatusType.AKTUELL),
            arenaTiltak(ARBTREN, ArenaDeltakerStatusType.AVSLAG),
            arenaTiltak(ARBTREN, ArenaDeltakerStatusType.GJENN_AVL),
            arenaTiltak(ARBTREN, ArenaDeltakerStatusType.IKKAKTUELL),
            arenaTiltak(ARBTREN, ArenaDeltakerStatusType.INFOMOETE),
            arenaTiltak(ARBTREN, ArenaDeltakerStatusType.NEITAKK),
            arenaTiltak(ARBTREN, ArenaDeltakerStatusType.VENTELISTE),
        )

        val tiltakListe = routesService.hentTiltakForSøknad("123", "correlationId")

        tiltakListe.size shouldBe 12
        tiltakListe.map {
            println(it.deltakelseStatus)
            it.deltakelseStatus
        } shouldNotContain listOf(
            IKKE_AKTUELL,
            FEILREGISTRERT,
            PABEGYNT_REGISTRERING,
            SOKT_INN,
            VENTELISTE,
            VURDERES,
        )
    }

    @Test
    fun `tiltak fra arena med status GJENN gir DELTAR hvis startdato har passert og VENTER_PÅ_OPPSTART hvis ikke`() {
        val routesService = RoutesService(
            kometClient = kometClient,
            arenaClient = arenaClient,
            gjennomforingRepo = gjennomforingRepo,
        )

        coEvery { kometClient.hentTiltakDeltagelser(any(), any()) } returns emptyList()
        val arenaTiltak1 = arenaTiltak(
            tiltak = HOYEREUTD,
            status = ArenaDeltakerStatusType.GJENN,
            LocalDate.now().plusDays(10),
            LocalDate.now().plusDays(20),
        )
        val arenaTiltak2 = arenaTiltak(
            tiltak = AMOB,
            status = ArenaDeltakerStatusType.GJENN,
            LocalDate.now().minusDays(20),
            LocalDate.now().minusDays(10),
        )
        coEvery { arenaClient.hentTiltakArena(any(), any()) } returns listOf(
            arenaTiltak1,
            arenaTiltak2,
        )
        routesService.hentTiltakForSaksbehandling("123", "correlationId").also { actual ->
            actual.size shouldBe 2
            actual[0].deltakelseStatus shouldBe VENTER_PA_OPPSTART
            actual[1].deltakelseStatus shouldBe DELTAR
        }

        routesService.hentTiltakForSøknad("123", "correlationId").also { actual ->
            // Vi filtrer ut AMOE,AMOB,AMOY
            actual.size shouldBe 1
            actual[0].deltakelseStatus shouldBe VENTER_PA_OPPSTART
        }
    }

    @Test
    fun `tiltak fra komet og arena som gir rett på tiltakspenger returnerer true`() {
        val routesService = RoutesService(
            kometClient = kometClient,
            arenaClient = arenaClient,
            gjennomforingRepo = gjennomforingRepo,
        )

        coEvery { kometClient.hentTiltakDeltagelser(any(), any()) } returns listOf(
            kometDeltaker("ARBFORB", KometDeltakerStatusType.DELTAR),
        )
        coEvery { arenaClient.hentTiltakArena(any(), any()) } returns listOf(
            arenaTiltak(ARBTREN, ArenaDeltakerStatusType.JATAKK),
        )
        routesService.hentTiltakForSaksbehandling("123", "correlationId").also {
            it.size shouldBe 2
            it[0].gjennomforing.arenaKode.rettPåTiltakspenger shouldBe true
            it[1].gjennomforing.arenaKode.rettPåTiltakspenger shouldBe true
        }
        routesService.hentTiltakForSøknad("123", "correlationId").also {
            it.size shouldBe 2
            it[0].gjennomforing.arenaKode.rettPåTiltakspenger shouldBe true
            it[1].gjennomforing.arenaKode.rettPåTiltakspenger shouldBe true
        }
    }

    @Test
    fun `tiltak fra komet og arena som ikke gir rett på tiltakspenger returnerer false`() {
        val routesService = RoutesService(
            kometClient = kometClient,
            arenaClient = arenaClient,
            gjennomforingRepo = gjennomforingRepo,
        )

        coEvery { kometClient.hentTiltakDeltagelser(any(), any()) } returns listOf(
            kometDeltaker("VASV", KometDeltakerStatusType.DELTAR),
        )
        coEvery { arenaClient.hentTiltakArena(any(), any()) } returns listOf(
            arenaTiltak(KURS, ArenaDeltakerStatusType.JATAKK),
        )

        routesService.hentTiltakForSaksbehandling("123", "correlationId").also {
            it.size shouldBe 2
            it.first { it.gjennomforing.arenaKode == TiltakResponsDTO.TiltakType.KURS }.gjennomforing.arenaKode.rettPåTiltakspenger shouldBe false
            it.first { it.gjennomforing.arenaKode == TiltakResponsDTO.TiltakType.VASV }.gjennomforing.arenaKode.rettPåTiltakspenger shouldBe false
        }

        routesService.hentTiltakForSøknad("123", "correlationId").also {
            it.size shouldBe 0
        }
    }

    @Test
    fun `tiltak med status som skal dukke opp i søknaden gir rett til å søke`() {
        val routesService = RoutesService(
            kometClient = kometClient,
            arenaClient = arenaClient,
            gjennomforingRepo = gjennomforingRepo,
        )

        coEvery { kometClient.hentTiltakDeltagelser(any(), any()) } returns listOf(
            kometDeltaker("ARBFORB", KometDeltakerStatusType.AVBRUTT),
            kometDeltaker("ARBFORB", KometDeltakerStatusType.FULLFORT),
            kometDeltaker("ARBFORB", KometDeltakerStatusType.DELTAR),
            kometDeltaker("ARBFORB", KometDeltakerStatusType.VENTER_PA_OPPSTART),
            kometDeltaker("ARBFORB", KometDeltakerStatusType.HAR_SLUTTET),
        )
        coEvery { arenaClient.hentTiltakArena(any(), any()) } returns listOf(
            arenaTiltak(ARBTREN, ArenaDeltakerStatusType.DELAVB),
            arenaTiltak(ARBTREN, ArenaDeltakerStatusType.FULLF),
            arenaTiltak(ARBTREN, ArenaDeltakerStatusType.GJENN),
            arenaTiltak(ARBTREN, ArenaDeltakerStatusType.GJENN_AVB),
            arenaTiltak(ARBTREN, ArenaDeltakerStatusType.IKKEM),
            arenaTiltak(ARBTREN, ArenaDeltakerStatusType.JATAKK),
            arenaTiltak(ARBTREN, ArenaDeltakerStatusType.TILBUD),
        )

        routesService.hentTiltakForSaksbehandling("123", "correlationId").also {
            it.size shouldBe 12
            it.all { it.deltakelseStatus.rettTilÅSøke }
            it.all { it.gjennomforing.arenaKode.rettPåTiltakspenger == true }
        }

        routesService.hentTiltakForSøknad("123", "correlationId").also {
            it.size shouldBe 12
            it.all { it.deltakelseStatus.rettTilÅSøke }
            it.all { it.gjennomforing.arenaKode.rettPåTiltakspenger }
        }
    }

    @Test
    fun `tiltak med status som ikke skal dukke opp i søknaden gir ikke rett til å søke`() {
        val routesService = RoutesService(
            kometClient = kometClient,
            arenaClient = arenaClient,
            gjennomforingRepo = gjennomforingRepo,
        )

        coEvery { kometClient.hentTiltakDeltagelser(any(), any()) } returns listOf(
            kometDeltaker("ARBFORB", KometDeltakerStatusType.IKKE_AKTUELL),
            kometDeltaker("ARBFORB", KometDeltakerStatusType.VURDERES),
            kometDeltaker("ARBFORB", KometDeltakerStatusType.FEILREGISTRERT),
            kometDeltaker("ARBFORB", KometDeltakerStatusType.PABEGYNT_REGISTRERING),
            kometDeltaker("ARBFORB", KometDeltakerStatusType.SOKT_INN),
            kometDeltaker("ARBFORB", KometDeltakerStatusType.VENTELISTE),
        )
        coEvery { arenaClient.hentTiltakArena(any(), any()) } returns listOf(
            arenaTiltak(AMO, ArenaDeltakerStatusType.AKTUELL),
            arenaTiltak(AMO, ArenaDeltakerStatusType.AVSLAG),
            arenaTiltak(AMO, ArenaDeltakerStatusType.GJENN_AVL),
            arenaTiltak(AMO, ArenaDeltakerStatusType.IKKAKTUELL),
            arenaTiltak(AMO, ArenaDeltakerStatusType.INFOMOETE),
            arenaTiltak(AMO, ArenaDeltakerStatusType.NEITAKK),
            arenaTiltak(AMO, ArenaDeltakerStatusType.VENTELISTE),
        )

        routesService.hentTiltakForSaksbehandling("123", "correlationId").also {
            it.size shouldBe 13
            it.all { !it.deltakelseStatus.rettTilÅSøke }
        }
        routesService.hentTiltakForSøknad("123", "correlationId").also {
            it.size shouldBe 0
        }
    }
}

private fun arenaTiltak(
    tiltak: TiltakType,
    status: ArenaDeltakerStatusType,
    fom: LocalDate = LocalDate.of(2023, 1, 1),
    tom: LocalDate = LocalDate.of(2023, 3, 31),
): ArenaTiltaksaktivitetResponsDTO.TiltaksaktivitetDTO {
    return ArenaTiltaksaktivitetResponsDTO.TiltaksaktivitetDTO(
        tiltakType = tiltak,
        aktivitetId = "solet",
        tiltakLokaltNavn = "LokaltNavn",
        arrangoer = "arrangoerNavn",
        bedriftsnummer = "123",
        deltakelsePeriode = ArenaTiltaksaktivitetResponsDTO.DeltakelsesPeriodeDTO(
            fom,
            tom,
        ),
        deltakelseProsent = 100F,
        deltakerStatusType = status,
        statusSistEndret = LocalDate.now(),
        begrunnelseInnsoeking = "begrunnelse",
        antallDagerPerUke = 2F,
    )
}

private fun kometDeltaker(type: String, status: KometDeltakerStatusType): KometResponseJson {
    return KometResponseJson(
        id = "id",
        gjennomforing = KometResponseJson.GjennomforingDTO(
            id = UUID.randomUUID().toString(),
            navn = "navn",
            type = type,
            tiltakstypeNavn = "tiltakstypeNavn",
            arrangor = KometResponseJson.GjennomforingDTO.ArrangorDTO(virksomhetsnummer = "123", navn = "arrangor"),
        ),
        startDato = LocalDate.of(2023, 1, 1),
        sluttDato = LocalDate.of(2023, 3, 31),
        status = status,
        dagerPerUke = 2F,
        prosentStilling = 100F,
        registrertDato = LocalDateTime.now(),
    )
}
