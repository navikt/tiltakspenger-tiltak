package no.nav.tiltakspenger.tiltak.services

import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.tiltakspenger.libs.arena.tiltak.ArenaTiltaksaktivitetResponsDTO
import no.nav.tiltakspenger.libs.arena.tiltak.ArenaTiltaksaktivitetResponsDTO.DeltakerStatusType
import no.nav.tiltakspenger.libs.arena.tiltak.ArenaTiltaksaktivitetResponsDTO.TiltakType.AMO
import no.nav.tiltakspenger.libs.arena.tiltak.ArenaTiltaksaktivitetResponsDTO.TiltakType.KURS
import no.nav.tiltakspenger.libs.tiltak.TiltakResponsDTO.DeltakerStatusResponseDTO
import no.nav.tiltakspenger.libs.tiltak.TiltakResponsDTO.TiltakType
import no.nav.tiltakspenger.tiltak.clients.arena.ArenaClient
import no.nav.tiltakspenger.tiltak.clients.komet.ArrangorDTO
import no.nav.tiltakspenger.tiltak.clients.komet.DeltakerDTO
import no.nav.tiltakspenger.tiltak.clients.komet.DeltakerStatusDTO
import no.nav.tiltakspenger.tiltak.clients.komet.GjennomforingDTO
import no.nav.tiltakspenger.tiltak.clients.komet.KometClient
import no.nav.tiltakspenger.tiltak.clients.tiltak.TiltakClient
import no.nav.tiltakspenger.tiltak.clients.valp.TiltaksgjennomforingOppstartstype
import no.nav.tiltakspenger.tiltak.clients.valp.Tiltaksgjennomforingsstatus
import no.nav.tiltakspenger.tiltak.clients.valp.Tiltakstype
import no.nav.tiltakspenger.tiltak.clients.valp.ValpClient
import no.nav.tiltakspenger.tiltak.clients.valp.ValpDTO
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

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
        coEvery { valpClient.hentTiltakGjennomføring(any()) } returns valpGjennomføring("VASV")
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
        coEvery { valpClient.hentTiltakGjennomføring(any()) } returns valpGjennomføring("ARBFORB")
        coEvery { arenaClient.hentTiltakArena(any()) } returns listOf(
            // Disse skal være med
            arenaTiltak(AMO, DeltakerStatusType.DELAVB),
            arenaTiltak(AMO, DeltakerStatusType.FULLF),
            arenaTiltak(AMO, DeltakerStatusType.GJENN),
            arenaTiltak(AMO, DeltakerStatusType.GJENN_AVB),
            arenaTiltak(AMO, DeltakerStatusType.IKKEM),
            arenaTiltak(AMO, DeltakerStatusType.JATAKK),
            arenaTiltak(AMO, DeltakerStatusType.TILBUD),

            // Disse skal ikke være med
            arenaTiltak(ArenaTiltaksaktivitetResponsDTO.TiltakType.AMO, DeltakerStatusType.AKTUELL),
            arenaTiltak(ArenaTiltaksaktivitetResponsDTO.TiltakType.AMO, DeltakerStatusType.AVSLAG),
            arenaTiltak(ArenaTiltaksaktivitetResponsDTO.TiltakType.AMO, DeltakerStatusType.GJENN_AVL),
            arenaTiltak(ArenaTiltaksaktivitetResponsDTO.TiltakType.AMO, DeltakerStatusType.IKKAKTUELL),
            arenaTiltak(ArenaTiltaksaktivitetResponsDTO.TiltakType.AMO, DeltakerStatusType.INFOMOETE),
            arenaTiltak(ArenaTiltaksaktivitetResponsDTO.TiltakType.AMO, DeltakerStatusType.NEITAKK),
            arenaTiltak(ArenaTiltaksaktivitetResponsDTO.TiltakType.AMO, DeltakerStatusType.VENTELISTE),
        )

        val tiltakListe = routesService.hentTiltakForSøknad("123")

        tiltakListe.size shouldBe 12
        tiltakListe.map {
            println(it.status)
            it.status
        } shouldNotContain listOf(
            DeltakerStatusResponseDTO.IKKE_AKTUELL,
            DeltakerStatusResponseDTO.FEILREGISTRERT,
            DeltakerStatusResponseDTO.PABEGYNT_REGISTRERING,
            DeltakerStatusResponseDTO.SOKT_INN,
            DeltakerStatusResponseDTO.VENTELISTE,
            DeltakerStatusResponseDTO.VURDERES,
        )
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
        coEvery { valpClient.hentTiltakGjennomføring(any()) } returns valpGjennomføring("ARBFORB")
        coEvery { arenaClient.hentTiltakArena(any()) } returns listOf(
            arenaTiltak(AMO, DeltakerStatusType.JATAKK),
        )

        val tiltakListe = routesService.hentAlleTiltak("123")

        tiltakListe.size shouldBe 2
        tiltakListe.first { it.gjennomforing.arenaKode == TiltakType.ARBFORB }.gjennomforing.arenaKode.rettPåTiltakspenger shouldBe true
        tiltakListe.first { it.gjennomforing.arenaKode == TiltakType.AMO }.gjennomforing.arenaKode.rettPåTiltakspenger shouldBe true
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
        coEvery { valpClient.hentTiltakGjennomføring(any()) } returns valpGjennomføring("VASV")
        coEvery { arenaClient.hentTiltakArena(any()) } returns listOf(
            arenaTiltak(ArenaTiltaksaktivitetResponsDTO.TiltakType.KURS, DeltakerStatusType.JATAKK),
        )

        val tiltakListe = routesService.hentAlleTiltak("123")

        tiltakListe.size shouldBe 2
        tiltakListe.first { it.gjennomforing.arenaKode == TiltakType.VASV }.gjennomforing.arenaKode.rettPåTiltakspenger shouldBe false
        tiltakListe.first { it.gjennomforing.arenaKode == TiltakType.KURS }.gjennomforing.arenaKode.rettPåTiltakspenger shouldBe false
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
        coEvery { valpClient.hentTiltakGjennomføring(any()) } returns valpGjennomføring("ARBFORB")
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
        tiltakListe.all { it.status.girRettTilÅASøke }
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
        coEvery { valpClient.hentTiltakGjennomføring(any()) } returns valpGjennomføring("ARBFORB")
        coEvery { arenaClient.hentTiltakArena(any()) } returns listOf(
            arenaTiltak(ArenaTiltaksaktivitetResponsDTO.TiltakType.AMO, DeltakerStatusType.AKTUELL),
            arenaTiltak(ArenaTiltaksaktivitetResponsDTO.TiltakType.AMO, DeltakerStatusType.AVSLAG),
            arenaTiltak(ArenaTiltaksaktivitetResponsDTO.TiltakType.AMO, DeltakerStatusType.GJENN_AVL),
            arenaTiltak(ArenaTiltaksaktivitetResponsDTO.TiltakType.AMO, DeltakerStatusType.IKKAKTUELL),
            arenaTiltak(ArenaTiltaksaktivitetResponsDTO.TiltakType.AMO, DeltakerStatusType.INFOMOETE),
            arenaTiltak(ArenaTiltaksaktivitetResponsDTO.TiltakType.AMO, DeltakerStatusType.NEITAKK),
            arenaTiltak(ArenaTiltaksaktivitetResponsDTO.TiltakType.AMO, DeltakerStatusType.VENTELISTE),
        )

        val tiltakListe = routesService.hentAlleTiltak("123")

        tiltakListe.size shouldBe 13
        tiltakListe.all { !it.status.girRettTilÅASøke }
    }
}

private fun arenaTiltak(tiltak: ArenaTiltaksaktivitetResponsDTO.TiltakType, status: DeltakerStatusType): ArenaTiltaksaktivitetResponsDTO.TiltaksaktivitetDTO {
    return ArenaTiltaksaktivitetResponsDTO.TiltaksaktivitetDTO(
        tiltakType = tiltak,
        aktivitetId = "solet",
        tiltakLokaltNavn = "LokaltNavn",
        arrangoer = "arrangoerNavn",
        bedriftsnummer = "123",
        deltakelsePeriode = ArenaTiltaksaktivitetResponsDTO.DeltakelsesPeriodeDTO(
            LocalDate.of(2023, 1, 1),
            LocalDate.of(2023, 3, 31),
        ),
        deltakelseProsent = 100F,
        deltakerStatusType = status,
        statusSistEndret = LocalDate.now(),
        begrunnelseInnsoeking = "begrunnelse",
        antallDagerPerUke = 2F,
    )
}
private fun valpGjennomføring(arenakode: String): ValpDTO {
    return ValpDTO(
        id = UUID.randomUUID(),
        tiltakstype = Tiltakstype(
            id = UUID.randomUUID(),
            navn = "navn",
            arenaKode = arenakode,
        ),
        navn = "navn",
        startDato = LocalDate.of(2023, 1, 1),
        sluttDato = LocalDate.of(2023, 3, 31),
        status = Tiltaksgjennomforingsstatus.AVBRUTT,
        virksomhetsnummer = "123",
        oppstart = TiltaksgjennomforingOppstartstype.FELLES,
    )
}

private fun kometDeltaker(type: String, status: DeltakerStatusDTO): DeltakerDTO {
    return DeltakerDTO(
        id = "id",
        gjennomforing = GjennomforingDTO(
            id = "id",
            navn = "navn",
            type = type,
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
