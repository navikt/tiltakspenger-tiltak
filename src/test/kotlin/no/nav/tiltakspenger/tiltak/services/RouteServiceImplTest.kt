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
import no.nav.tiltakspenger.tiltak.clients.tiltak.TiltakClient
import no.nav.tiltakspenger.tiltak.clients.valp.ValpClient
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

internal class RouteServiceImplTest {
    private val kometClient = mockk<KometClient>()
    private val valpClient = mockk<ValpClient>()
    private val tiltakClient = mockk<TiltakClient>()
    private val arenaClient = mockk<ArenaClient>()

    @Test
    fun `tiltak som ikke gir rett til Tiltakspenger er ikke med i søknaden selv om de har riktig status`() {
        val routesService = RouteServiceImpl(
            kometClient = kometClient,
            valpClient = valpClient,
            tiltakClient = tiltakClient,
            arenaClient = arenaClient,
        )

        coEvery { kometClient.hentTiltakDeltagelser(any()) } returns listOf(
            kometDeltaker("VASV", DeltakerStatusDTO.AVBRUTT),
        )
        coEvery { arenaClient.hentTiltakArena(any()) } returns listOf(
            arenaTiltak(KURS, DeltakerStatusType.DELAVB),
        )

        val tiltakListe = routesService.hentTiltakForSøknad("123")

        tiltakListe.size shouldBe 0
    }

    @Test
    fun `tiltak fra komet og arena som ikke gir rett til å søke er ikke med i listen`() {
        val routesService = RouteServiceImpl(
            kometClient = kometClient,
            valpClient = valpClient,
            tiltakClient = tiltakClient,
            arenaClient = arenaClient,
        )

        coEvery { kometClient.hentTiltakDeltagelser(any()) } returns listOf(
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
        coEvery { arenaClient.hentTiltakArena(any()) } returns listOf(
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

        val tiltakListe = routesService.hentTiltakForSøknad("123")

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
    fun `tiltak fra arena med status GJENN gir DELTAR hvis startdato har passert og VENTER_PÅ_OPPSTART hvis ikke`() {
        val routesService = RouteServiceImpl(
            kometClient = kometClient,
            valpClient = valpClient,
            tiltakClient = tiltakClient,
            arenaClient = arenaClient,
        )

        coEvery { kometClient.hentTiltakDeltagelser(any()) } returns emptyList()
        coEvery { arenaClient.hentTiltakArena(any()) } returns listOf(
            arenaTiltak(tiltak = AMO, status = DeltakerStatusType.GJENN, LocalDate.now().plusDays(10), LocalDate.now().plusDays(20)),
            arenaTiltak(tiltak = AMOE, status = DeltakerStatusType.TILBUD, LocalDate.now().plusDays(10), LocalDate.now().plusDays(20)),
            arenaTiltak(tiltak = AMOB, status = DeltakerStatusType.GJENN, LocalDate.now().minusDays(20), LocalDate.now().minusDays(10)),
            arenaTiltak(tiltak = AMOY, status = DeltakerStatusType.TILBUD, LocalDate.now().minusDays(20), LocalDate.now().minusDays(10)),
        )

        val tiltakListe = routesService.hentAlleTiltak("123")

        tiltakListe.size shouldBe 4
        tiltakListe.first { it.gjennomforing.arenaKode == TiltakResponsDTO.TiltakType.AMO }.deltakelseStatus shouldBe TiltakResponsDTO.DeltakerStatusDTO.VENTER_PA_OPPSTART
        tiltakListe.first { it.gjennomforing.arenaKode == TiltakResponsDTO.TiltakType.AMOE }.deltakelseStatus shouldBe TiltakResponsDTO.DeltakerStatusDTO.VENTER_PA_OPPSTART
        tiltakListe.first { it.gjennomforing.arenaKode == TiltakResponsDTO.TiltakType.AMOB }.deltakelseStatus shouldBe TiltakResponsDTO.DeltakerStatusDTO.DELTAR
        tiltakListe.first { it.gjennomforing.arenaKode == TiltakResponsDTO.TiltakType.AMOY }.deltakelseStatus shouldBe TiltakResponsDTO.DeltakerStatusDTO.DELTAR
    }

    @Test
    fun `tiltak fra komet og arena som gir rett på tiltakspenger returnerer true`() {
        val routesService = RouteServiceImpl(
            kometClient = kometClient,
            valpClient = valpClient,
            tiltakClient = tiltakClient,
            arenaClient = arenaClient,
        )

        coEvery { kometClient.hentTiltakDeltagelser(any()) } returns listOf(
            kometDeltaker("ARBFORB", DeltakerStatusDTO.DELTAR),
        )
        coEvery { arenaClient.hentTiltakArena(any()) } returns listOf(
            arenaTiltak(ARBTREN, DeltakerStatusType.JATAKK),
        )

        val tiltakListe = routesService.hentAlleTiltak("123")

        tiltakListe.size shouldBe 2
        tiltakListe.first { it.gjennomforing.arenaKode == TiltakResponsDTO.TiltakType.ARBFORB }.gjennomforing.arenaKode.rettPåTiltakspenger shouldBe true
        tiltakListe.first { it.gjennomforing.arenaKode == TiltakResponsDTO.TiltakType.ARBTREN }.gjennomforing.arenaKode.rettPåTiltakspenger shouldBe true
    }

    @Test
    fun `tiltak fra komet og arena som ikke gir rett på tiltakspenger returnerer false`() {
        val routesService = RouteServiceImpl(
            kometClient = kometClient,
            valpClient = valpClient,
            tiltakClient = tiltakClient,
            arenaClient = arenaClient,
        )

        coEvery { kometClient.hentTiltakDeltagelser(any()) } returns listOf(
            kometDeltaker("VASV", DeltakerStatusDTO.DELTAR),
        )
        coEvery { arenaClient.hentTiltakArena(any()) } returns listOf(
            arenaTiltak(KURS, DeltakerStatusType.JATAKK),
        )

        val tiltakListe = routesService.hentAlleTiltak("123")

        tiltakListe.size shouldBe 2
        tiltakListe.first { it.gjennomforing.arenaKode == TiltakResponsDTO.TiltakType.VASV }.gjennomforing.arenaKode.rettPåTiltakspenger shouldBe false
        tiltakListe.first { it.gjennomforing.arenaKode == TiltakResponsDTO.TiltakType.KURS }.gjennomforing.arenaKode.rettPåTiltakspenger shouldBe false
    }

    @Test
    fun `tiltak med status som skal dukke opp i søknaden gir rett til å søke`() {
        val routesService = RouteServiceImpl(
            kometClient = kometClient,
            valpClient = valpClient,
            tiltakClient = tiltakClient,
            arenaClient = arenaClient,
        )

        coEvery { kometClient.hentTiltakDeltagelser(any()) } returns listOf(
            kometDeltaker("ARBFORB", DeltakerStatusDTO.AVBRUTT),
            kometDeltaker("ARBFORB", DeltakerStatusDTO.FULLFORT),
            kometDeltaker("ARBFORB", DeltakerStatusDTO.DELTAR),
            kometDeltaker("ARBFORB", DeltakerStatusDTO.VENTER_PA_OPPSTART),
            kometDeltaker("ARBFORB", DeltakerStatusDTO.HAR_SLUTTET),
        )
        coEvery { arenaClient.hentTiltakArena(any()) } returns listOf(
            arenaTiltak(AMO, DeltakerStatusType.DELAVB),
            arenaTiltak(AMO, DeltakerStatusType.FULLF),
            arenaTiltak(AMO, DeltakerStatusType.GJENN),
            arenaTiltak(AMO, DeltakerStatusType.GJENN_AVB),
            arenaTiltak(AMO, DeltakerStatusType.IKKEM),
            arenaTiltak(AMO, DeltakerStatusType.JATAKK),
            arenaTiltak(AMO, DeltakerStatusType.TILBUD),
        )

        val tiltakListe = routesService.hentAlleTiltak("123")

        tiltakListe.size shouldBe 12
        tiltakListe.all { it.deltakelseStatus.rettTilÅSøke }
    }

    @Test
    fun `tiltak med status som ikke skal dukke opp i søknaden gir ikke rett til å søke`() {
        val routesService = RouteServiceImpl(
            kometClient = kometClient,
            valpClient = valpClient,
            tiltakClient = tiltakClient,
            arenaClient = arenaClient,
        )

        coEvery { kometClient.hentTiltakDeltagelser(any()) } returns listOf(
            kometDeltaker("ARBFORB", DeltakerStatusDTO.IKKE_AKTUELL),
            kometDeltaker("ARBFORB", DeltakerStatusDTO.VURDERES),
            kometDeltaker("ARBFORB", DeltakerStatusDTO.FEILREGISTRERT),
            kometDeltaker("ARBFORB", DeltakerStatusDTO.PABEGYNT_REGISTRERING),
            kometDeltaker("ARBFORB", DeltakerStatusDTO.SOKT_INN),
            kometDeltaker("ARBFORB", DeltakerStatusDTO.VENTELISTE),
        )
        coEvery { arenaClient.hentTiltakArena(any()) } returns listOf(
            arenaTiltak(AMO, DeltakerStatusType.AKTUELL),
            arenaTiltak(AMO, DeltakerStatusType.AVSLAG),
            arenaTiltak(AMO, DeltakerStatusType.GJENN_AVL),
            arenaTiltak(AMO, DeltakerStatusType.IKKAKTUELL),
            arenaTiltak(AMO, DeltakerStatusType.INFOMOETE),
            arenaTiltak(AMO, DeltakerStatusType.NEITAKK),
            arenaTiltak(AMO, DeltakerStatusType.VENTELISTE),
        )

        val tiltakListe = routesService.hentAlleTiltak("123")

        tiltakListe.size shouldBe 13
        tiltakListe.all { !it.deltakelseStatus.rettTilÅSøke }
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
