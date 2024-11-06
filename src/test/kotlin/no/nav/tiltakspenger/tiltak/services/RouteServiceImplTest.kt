package no.nav.tiltakspenger.tiltak.services

import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.tiltakspenger.libs.arena.tiltak.ArenaTiltaksaktivitetResponsDTO
import no.nav.tiltakspenger.libs.arena.tiltak.ArenaTiltaksaktivitetResponsDTO.DeltakerStatusType
import no.nav.tiltakspenger.libs.arena.tiltak.ArenaTiltaksaktivitetResponsDTO.TiltakType
import no.nav.tiltakspenger.libs.arena.tiltak.ArenaTiltaksaktivitetResponsDTO.TiltakType.AMO
import no.nav.tiltakspenger.libs.arena.tiltak.ArenaTiltaksaktivitetResponsDTO.TiltakType.AMOB
import no.nav.tiltakspenger.libs.arena.tiltak.ArenaTiltaksaktivitetResponsDTO.TiltakType.AMOE
import no.nav.tiltakspenger.libs.arena.tiltak.ArenaTiltaksaktivitetResponsDTO.TiltakType.AMOY
import no.nav.tiltakspenger.libs.arena.tiltak.ArenaTiltaksaktivitetResponsDTO.TiltakType.ARBTREN
import no.nav.tiltakspenger.libs.arena.tiltak.ArenaTiltaksaktivitetResponsDTO.TiltakType.KURS
import no.nav.tiltakspenger.libs.tiltak.TiltakResponsDTO
import no.nav.tiltakspenger.tiltak.clients.arena.ArenaClient
import no.nav.tiltakspenger.tiltak.clients.komet.ArrangorDTO
import no.nav.tiltakspenger.tiltak.clients.komet.DeltakerDTO
import no.nav.tiltakspenger.tiltak.clients.komet.DeltakerStatusDTO
import no.nav.tiltakspenger.tiltak.clients.komet.GjennomforingDTO
import no.nav.tiltakspenger.tiltak.clients.komet.KometClient
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

internal class RouteServiceImplTest {
    private val kometClient = mockk<KometClient>()
    private val arenaClient = mockk<ArenaClient>()

    @Test
    fun `tiltak som ikke gir rett til Tiltakspenger er ikke med i søknaden selv om de har riktig status`() {
        val routesService = RouteServiceImpl(
            kometClient = kometClient,
            arenaClient = arenaClient,
        )

        coEvery { kometClient.hentTiltakDeltagelser(any(), any()) } returns listOf(
            kometDeltaker("VASV", DeltakerStatusDTO.AVBRUTT),
        )
        coEvery { arenaClient.hentTiltakArena(any(), any()) } returns listOf(
            arenaTiltak(KURS, DeltakerStatusType.DELAVB),
        )

        val tiltakListe = routesService.hentTiltakForSøknad("123", "correlationId")

        tiltakListe.size shouldBe 0
    }

    @Test
    fun `tiltak fra komet og arena som ikke gir rett til å søke er ikke med i listen`() {
        val routesService = RouteServiceImpl(
            kometClient = kometClient,
            arenaClient = arenaClient,
        )

        coEvery { kometClient.hentTiltakDeltagelser(any(), any()) } returns listOf(
            // Disse skal være med...
            kometDeltaker("ARBFORB", DeltakerStatusDTO.AVBRUTT),
            kometDeltaker("ARBFORB", DeltakerStatusDTO.FULLFORT),
            kometDeltaker("ARBFORB", DeltakerStatusDTO.DELTAR),
            kometDeltaker("ARBFORB", DeltakerStatusDTO.VENTER_PA_OPPSTART),
            kometDeltaker("ARBFORB", DeltakerStatusDTO.HAR_SLUTTET),
            // Disse skal ikke være med
            kometDeltaker("ARBFORB", DeltakerStatusDTO.IKKE_AKTUELL),
            kometDeltaker("ARBFORB", DeltakerStatusDTO.VURDERES),
            kometDeltaker("ARBFORB", DeltakerStatusDTO.FEILREGISTRERT),
            kometDeltaker("ARBFORB", DeltakerStatusDTO.PABEGYNT_REGISTRERING),
            kometDeltaker("ARBFORB", DeltakerStatusDTO.SOKT_INN),
            kometDeltaker("ARBFORB", DeltakerStatusDTO.VENTELISTE),
        )
        coEvery { arenaClient.hentTiltakArena(any(), any()) } returns listOf(
            // Disse skal være med
            arenaTiltak(ARBTREN, DeltakerStatusType.DELAVB),
            arenaTiltak(ARBTREN, DeltakerStatusType.FULLF),
            arenaTiltak(ARBTREN, DeltakerStatusType.GJENN),
            arenaTiltak(ARBTREN, DeltakerStatusType.GJENN_AVB),
            arenaTiltak(ARBTREN, DeltakerStatusType.IKKEM),
            arenaTiltak(ARBTREN, DeltakerStatusType.JATAKK),
            arenaTiltak(ARBTREN, DeltakerStatusType.TILBUD),

            // Disse skal ikke være med
            arenaTiltak(ARBTREN, DeltakerStatusType.AKTUELL),
            arenaTiltak(ARBTREN, DeltakerStatusType.AVSLAG),
            arenaTiltak(ARBTREN, DeltakerStatusType.GJENN_AVL),
            arenaTiltak(ARBTREN, DeltakerStatusType.IKKAKTUELL),
            arenaTiltak(ARBTREN, DeltakerStatusType.INFOMOETE),
            arenaTiltak(ARBTREN, DeltakerStatusType.NEITAKK),
            arenaTiltak(ARBTREN, DeltakerStatusType.VENTELISTE),
        )

        val tiltakListe = routesService.hentTiltakForSøknad("123", "correlationId")

        tiltakListe.size shouldBe 12
        tiltakListe.map {
            println(it.deltakelseStatus)
            it.deltakelseStatus
        } shouldNotContain listOf(
            TiltakResponsDTO.DeltakerStatusDTO.IKKE_AKTUELL,
            TiltakResponsDTO.DeltakerStatusDTO.FEILREGISTRERT,
            TiltakResponsDTO.DeltakerStatusDTO.PABEGYNT_REGISTRERING,
            TiltakResponsDTO.DeltakerStatusDTO.SOKT_INN,
            TiltakResponsDTO.DeltakerStatusDTO.VENTELISTE,
            TiltakResponsDTO.DeltakerStatusDTO.VURDERES,
        )
    }

    @Test
    fun `tiltak fra arena med status GJENN og TILBUD gir DELTAR hvis startdato har passert og VENTER_PÅ_OPPSTART hvis ikke`() {
        val routesService = RouteServiceImpl(
            kometClient = kometClient,
            arenaClient = arenaClient,
        )

        coEvery { kometClient.hentTiltakDeltagelser(any(), any()) } returns emptyList()
        val arenaTiltak1 = arenaTiltak(
            tiltak = AMO,
            status = DeltakerStatusType.GJENN,
            LocalDate.now().plusDays(10),
            LocalDate.now().plusDays(20),
        )
        val arenaTiltak2 = arenaTiltak(
            tiltak = AMOE,
            status = DeltakerStatusType.TILBUD,
            LocalDate.now().plusDays(10),
            LocalDate.now().plusDays(20),
        )
        val arenaTiltak3 = arenaTiltak(
            tiltak = AMOB,
            status = DeltakerStatusType.GJENN,
            LocalDate.now().minusDays(20),
            LocalDate.now().minusDays(10),
        )
        val arenaTiltak4 = arenaTiltak(
            tiltak = AMOY,
            status = DeltakerStatusType.TILBUD,
            LocalDate.now().minusDays(20),
            LocalDate.now().minusDays(10),
        )
        coEvery { arenaClient.hentTiltakArena(any(), any()) } returns listOf(
            arenaTiltak1,
            arenaTiltak2,
            arenaTiltak3,
            arenaTiltak4,
        )
        routesService.hentTiltakForSaksbehandling("123", "correlationId").also { actual ->
            actual.size shouldBe 4
            actual[0].deltakelseStatus shouldBe TiltakResponsDTO.DeltakerStatusDTO.VENTER_PA_OPPSTART
            actual[1].deltakelseStatus shouldBe TiltakResponsDTO.DeltakerStatusDTO.VENTER_PA_OPPSTART
            actual[2].deltakelseStatus shouldBe TiltakResponsDTO.DeltakerStatusDTO.DELTAR
            actual[3].deltakelseStatus shouldBe TiltakResponsDTO.DeltakerStatusDTO.DELTAR
        }

        routesService.hentTiltakForSøknad("123", "correlationId").also { actual ->
            // Vi filtrer ut AMO, AMOE;AMOB,AMOY
            // TODO post-mvp jah: Kanskje vi burde bruke noen tiltak i denne testen som ikke filtreres ut?
            actual.size shouldBe 0
        }
    }

    @Test
    fun `tiltak fra komet og arena som gir rett på tiltakspenger returnerer true`() {
        val routesService = RouteServiceImpl(
            kometClient = kometClient,
            arenaClient = arenaClient,
        )

        coEvery { kometClient.hentTiltakDeltagelser(any(), any()) } returns listOf(
            kometDeltaker("ARBFORB", DeltakerStatusDTO.DELTAR),
        )
        coEvery { arenaClient.hentTiltakArena(any(), any()) } returns listOf(
            arenaTiltak(ARBTREN, DeltakerStatusType.JATAKK),
        )
        routesService.hentTiltakForSaksbehandling("123", "correlationId").also {
            it.size shouldBe 2
            it[0].typeKode.rettPåTiltakspenger shouldBe true
            it[1].typeKode.rettPåTiltakspenger shouldBe true
        }
        routesService.hentTiltakForSøknad("123", "correlationId").also {
            it.size shouldBe 2
            it[0].gjennomforing.arenaKode.rettPåTiltakspenger shouldBe true
            it[1].gjennomforing.arenaKode.rettPåTiltakspenger shouldBe true
        }
    }

    @Test
    fun `tiltak fra komet og arena som ikke gir rett på tiltakspenger returnerer false`() {
        val routesService = RouteServiceImpl(
            kometClient = kometClient,
            arenaClient = arenaClient,
        )

        coEvery { kometClient.hentTiltakDeltagelser(any(), any()) } returns listOf(
            kometDeltaker("VASV", DeltakerStatusDTO.DELTAR),
        )
        coEvery { arenaClient.hentTiltakArena(any(), any()) } returns listOf(
            arenaTiltak(KURS, DeltakerStatusType.JATAKK),
        )

        routesService.hentTiltakForSaksbehandling("123", "correlationId").also {
            it.size shouldBe 2
            it.first { it.typeKode == TiltakResponsDTO.TiltakType.VASV }.typeKode.rettPåTiltakspenger shouldBe false
            it.first { it.typeKode == TiltakResponsDTO.TiltakType.KURS }.typeKode.rettPåTiltakspenger shouldBe false
        }

        routesService.hentTiltakForSøknad("123", "correlationId").also {
            it.size shouldBe 0
        }
    }

    @Test
    fun `tiltak med status som skal dukke opp i søknaden gir rett til å søke`() {
        val routesService = RouteServiceImpl(
            kometClient = kometClient,
            arenaClient = arenaClient,
        )

        coEvery { kometClient.hentTiltakDeltagelser(any(), any()) } returns listOf(
            kometDeltaker("ARBFORB", DeltakerStatusDTO.AVBRUTT),
            kometDeltaker("ARBFORB", DeltakerStatusDTO.FULLFORT),
            kometDeltaker("ARBFORB", DeltakerStatusDTO.DELTAR),
            kometDeltaker("ARBFORB", DeltakerStatusDTO.VENTER_PA_OPPSTART),
            kometDeltaker("ARBFORB", DeltakerStatusDTO.HAR_SLUTTET),
        )
        coEvery { arenaClient.hentTiltakArena(any(), any()) } returns listOf(
            arenaTiltak(AMO, DeltakerStatusType.DELAVB),
            arenaTiltak(AMO, DeltakerStatusType.FULLF),
            arenaTiltak(AMO, DeltakerStatusType.GJENN),
            arenaTiltak(AMO, DeltakerStatusType.GJENN_AVB),
            arenaTiltak(AMO, DeltakerStatusType.IKKEM),
            arenaTiltak(AMO, DeltakerStatusType.JATAKK),
            arenaTiltak(AMO, DeltakerStatusType.TILBUD),
        )

        routesService.hentTiltakForSaksbehandling("123", "correlationId").also {
            it.size shouldBe 12
            it.all { it.deltakelseStatus.rettTilÅSøke }
        }

        routesService.hentTiltakForSøknad("123", "correlationId").also {
            // TODO post-mvp jah: Her bør vi kanskje også bruke typer som faktisk gir rett til å søke?
            it.size shouldBe 5
        }
    }

    @Test
    fun `tiltak med status som ikke skal dukke opp i søknaden gir ikke rett til å søke`() {
        val routesService = RouteServiceImpl(
            kometClient = kometClient,
            arenaClient = arenaClient,
        )

        coEvery { kometClient.hentTiltakDeltagelser(any(), any()) } returns listOf(
            kometDeltaker("ARBFORB", DeltakerStatusDTO.IKKE_AKTUELL),
            kometDeltaker("ARBFORB", DeltakerStatusDTO.VURDERES),
            kometDeltaker("ARBFORB", DeltakerStatusDTO.FEILREGISTRERT),
            kometDeltaker("ARBFORB", DeltakerStatusDTO.PABEGYNT_REGISTRERING),
            kometDeltaker("ARBFORB", DeltakerStatusDTO.SOKT_INN),
            kometDeltaker("ARBFORB", DeltakerStatusDTO.VENTELISTE),
        )
        coEvery { arenaClient.hentTiltakArena(any(), any()) } returns listOf(
            arenaTiltak(AMO, DeltakerStatusType.AKTUELL),
            arenaTiltak(AMO, DeltakerStatusType.AVSLAG),
            arenaTiltak(AMO, DeltakerStatusType.GJENN_AVL),
            arenaTiltak(AMO, DeltakerStatusType.IKKAKTUELL),
            arenaTiltak(AMO, DeltakerStatusType.INFOMOETE),
            arenaTiltak(AMO, DeltakerStatusType.NEITAKK),
            arenaTiltak(AMO, DeltakerStatusType.VENTELISTE),
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
    status: DeltakerStatusType,
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

private fun kometDeltaker(type: String, status: DeltakerStatusDTO): DeltakerDTO {
    return DeltakerDTO(
        id = "id",
        gjennomforing = GjennomforingDTO(
            id = "id",
            navn = "navn",
            type = type,
            tiltakstypeNavn = "tiltakstypeNavn",
            arrangor = ArrangorDTO(virksomhetsnummer = "123", navn = "arrangor"),
        ),
        startDato = LocalDate.of(2023, 1, 1),
        sluttDato = LocalDate.of(2023, 3, 31),
        status = status,
        dagerPerUke = 2F,
        prosentStilling = 100F,
        registrertDato = LocalDateTime.now(),
    )
}
