package no.nav.tiltakspenger.tiltak.services

import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.tiltak.TiltakResponsDTO
import no.nav.tiltakspenger.libs.tiltak.TiltakResponsDTO.DeltakerStatusDTO.DELTAR
import no.nav.tiltakspenger.libs.tiltak.TiltakResponsDTO.DeltakerStatusDTO.VENTER_PA_OPPSTART
import no.nav.tiltakspenger.libs.tiltak.TiltakshistorikkDTO
import no.nav.tiltakspenger.tiltak.clients.tiltakshistorikk.TiltakshistorikkClient
import no.nav.tiltakspenger.tiltak.clients.tiltakshistorikk.dto.ArbeidsgiverAvtaleStatusDto
import no.nav.tiltakspenger.tiltak.clients.tiltakshistorikk.dto.ArenaDeltakerStatusDto
import no.nav.tiltakspenger.tiltak.clients.tiltakshistorikk.dto.KometDeltakerStatusDto
import no.nav.tiltakspenger.tiltak.clients.tiltakshistorikk.dto.TiltakshistorikkV1Dto
import no.nav.tiltakspenger.tiltak.clients.tiltakshistorikk.dto.TiltakshistorikkV1Response
import no.nav.tiltakspenger.tiltak.clients.tiltakshistorikk.dto.TiltakskodeDto
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tools.jackson.module.kotlin.readValue
import java.time.LocalDate
import java.util.UUID

class TiltakshistorikkServiceTest {
    private val tiltakshistorikkClient = mockk<TiltakshistorikkClient>()
    private val tiltakshistorikkService = TiltakshistorikkService(tiltakshistorikkClient)

    private val fnr = "12345678910"

    @BeforeEach
    fun cleanMocks() {
        clearMocks(tiltakshistorikkClient)
    }

    @Test
    fun `tiltakshistorikk inneholder deltakelser fra alle systemer - mappes korrekt`() {
        coEvery { tiltakshistorikkClient.hentTiltaksdeltakelser(any()) } returns getRespons().historikk

        val tiltakshistorikk = tiltakshistorikkService.hentTiltakshistorikkForSaksbehandling(fnr)

        tiltakshistorikk.size shouldBe 3
        val tiltakFraKomet = tiltakshistorikk.find { it.kilde == TiltakshistorikkDTO.Kilde.KOMET } ?: throw RuntimeException("Fant ikke komet-tiltak")
        tiltakFraKomet.id shouldBe "6d54228f-534f-4b4b-9160-65eae26a3b06"
        tiltakFraKomet.gjennomforing shouldBe TiltakshistorikkDTO.GjennomforingDTO(
            id = "9caf398e-8e38-41fc-af29-b7ee6f62205a",
            visningsnavn = "Arbeidsforberedende trening hos Arrangør",
            arrangornavn = "Arrangør",
            typeNavn = "Arbeidsforberedende trening",
            arenaKode = TiltakResponsDTO.TiltakType.ARBFORB,
            deltidsprosent = 100.0,
        )
        tiltakFraKomet.deltakelseFom shouldBe LocalDate.of(2024, 4, 4)
        tiltakFraKomet.deltakelseTom shouldBe LocalDate.of(2024, 4, 5)
        tiltakFraKomet.deltakelseStatus shouldBe TiltakResponsDTO.DeltakerStatusDTO.HAR_SLUTTET
        tiltakFraKomet.antallDagerPerUke shouldBe 3.0F
        tiltakFraKomet.deltakelseProsent shouldBe 60.0F

        val tiltakFraArena = tiltakshistorikk.find { it.kilde == TiltakshistorikkDTO.Kilde.ARENA } ?: throw RuntimeException("Fant ikke arena-tiltak")
        tiltakFraArena.id shouldBe "TA1234567"
        tiltakFraArena.gjennomforing shouldBe TiltakshistorikkDTO.GjennomforingDTO(
            id = "",
            visningsnavn = "Arbeidsmarkedsopplæring (enkeltplass) hos Arrangør",
            arrangornavn = "Arrangør",
            typeNavn = "Arbeidsmarkedsopplæring (enkeltplass)",
            arenaKode = TiltakResponsDTO.TiltakType.ENKELAMO,
            deltidsprosent = null,
        )
        tiltakFraArena.deltakelseFom shouldBe LocalDate.of(2024, 7, 3)
        tiltakFraArena.deltakelseTom shouldBe LocalDate.of(2024, 10, 31)
        tiltakFraArena.deltakelseStatus shouldBe DELTAR
        tiltakFraArena.antallDagerPerUke shouldBe 5.0F
        tiltakFraArena.deltakelseProsent shouldBe 100.0F

        val tiltakFraTeamTiltak = tiltakshistorikk.find { it.kilde == TiltakshistorikkDTO.Kilde.TEAM_TILTAK } ?: throw RuntimeException("Fant ikke team tiltak-tiltak")
        tiltakFraTeamTiltak.id shouldBe "9dea48c1-d494-4664-9427-bdb20a6f265f"
        tiltakFraTeamTiltak.gjennomforing shouldBe TiltakshistorikkDTO.GjennomforingDTO(
            id = "",
            visningsnavn = "Arbeidstrening hos Arbeidsgiver",
            arrangornavn = "Arbeidsgiver",
            typeNavn = "Arbeidstrening",
            arenaKode = TiltakResponsDTO.TiltakType.ARBTREN,
            deltidsprosent = null,
        )
        tiltakFraTeamTiltak.deltakelseFom shouldBe LocalDate.of(2024, 1, 1)
        tiltakFraTeamTiltak.deltakelseTom shouldBe LocalDate.of(2024, 12, 31)
        tiltakFraTeamTiltak.deltakelseStatus shouldBe DELTAR
        tiltakFraTeamTiltak.antallDagerPerUke shouldBe 5.0f
        tiltakFraTeamTiltak.deltakelseProsent shouldBe 100.0f
    }

    @Test
    fun `hentTiltakshistorikkForSoknad - tiltak som ikke gir rett er ikke med i søknaden selv om de har riktig status`() {
        coEvery { tiltakshistorikkClient.hentTiltaksdeltakelser(any()) } returns listOf(
            tiltakshistorikkKometTiltak(
                tiltak = TiltakshistorikkV1Dto.TeamKometDeltakelse.Tiltakstype(
                    tiltakskode = TiltakskodeDto.VARIG_TILRETTELAGT_ARBEID_SKJERMET,
                    navn = "Varig tilrettelagt arbeid",
                ),
                status = KometDeltakerStatusDto(
                    type = KometDeltakerStatusDto.DeltakerStatusType.AVBRUTT,
                ),
            ),
            tiltakshistorikkArenaTiltak(
                tiltak = TiltakshistorikkV1Dto.ArenaDeltakelse.Tiltakstype(
                    tiltakskode = TiltakResponsDTO.TiltakType.KURS.name,
                    navn = TiltakResponsDTO.TiltakType.KURS.navn,
                ),
                status = ArenaDeltakerStatusDto.DELTAKELSE_AVBRUTT,
            ),
            tiltakshistorikkTeamTiltakTiltak(
                tiltakstype = TiltakshistorikkV1Dto.TeamTiltakAvtale.Tiltakstype(
                    tiltakskode = TiltakshistorikkV1Dto.TeamTiltakAvtale.Tiltakskode.INKLUDERINGSTILSKUDD,
                    navn = "Inkluderingstilskudd",
                ),
                status = ArbeidsgiverAvtaleStatusDto.AVSLUTTET,
            ),
        )

        val tiltaksdeltakelser = tiltakshistorikkService.hentTiltakshistorikkForSoknad(fnr)

        tiltaksdeltakelser.size shouldBe 0
    }

    @Test
    fun `hentTiltakshistorikkForSoknad - statuser fra komet, arena og team tiltak som ikke gir rett til å søke er ikke med i listen`() {
        coEvery { tiltakshistorikkClient.hentTiltaksdeltakelser(any()) } returns listOf(
            // Disse skal være med...
            tiltakshistorikkKometTiltak(
                status = KometDeltakerStatusDto(type = KometDeltakerStatusDto.DeltakerStatusType.AVBRUTT),
            ),
            tiltakshistorikkKometTiltak(
                status = KometDeltakerStatusDto(type = KometDeltakerStatusDto.DeltakerStatusType.FULLFORT),
            ),
            tiltakshistorikkKometTiltak(
                status = KometDeltakerStatusDto(type = KometDeltakerStatusDto.DeltakerStatusType.DELTAR),
            ),
            tiltakshistorikkKometTiltak(
                status = KometDeltakerStatusDto(type = KometDeltakerStatusDto.DeltakerStatusType.VENTER_PA_OPPSTART),
            ),
            tiltakshistorikkKometTiltak(
                status = KometDeltakerStatusDto(type = KometDeltakerStatusDto.DeltakerStatusType.HAR_SLUTTET),
            ),
            tiltakshistorikkTeamTiltakTiltak(
                status = ArbeidsgiverAvtaleStatusDto.KLAR_FOR_OPPSTART,
            ),
            tiltakshistorikkTeamTiltakTiltak(
                status = ArbeidsgiverAvtaleStatusDto.GJENNOMFORES,
            ),
            tiltakshistorikkTeamTiltakTiltak(
                status = ArbeidsgiverAvtaleStatusDto.AVSLUTTET,
            ),
            tiltakshistorikkTeamTiltakTiltak(
                status = ArbeidsgiverAvtaleStatusDto.AVBRUTT,
            ),
            tiltakshistorikkArenaTiltak(
                status = ArenaDeltakerStatusDto.DELTAKELSE_AVBRUTT,
            ),
            tiltakshistorikkArenaTiltak(
                status = ArenaDeltakerStatusDto.FULLFORT,
            ),
            tiltakshistorikkArenaTiltak(
                status = ArenaDeltakerStatusDto.GJENNOMFORES,
            ),
            tiltakshistorikkArenaTiltak(
                status = ArenaDeltakerStatusDto.GJENNOMFORING_AVBRUTT,
            ),
            tiltakshistorikkArenaTiltak(
                status = ArenaDeltakerStatusDto.IKKE_MOTT,
            ),
            tiltakshistorikkArenaTiltak(
                status = ArenaDeltakerStatusDto.TAKKET_JA_TIL_TILBUD,
            ),
            tiltakshistorikkArenaTiltak(
                status = ArenaDeltakerStatusDto.TILBUD,
            ),
            // Disse skal ikke være med
            tiltakshistorikkKometTiltak(
                status = KometDeltakerStatusDto(type = KometDeltakerStatusDto.DeltakerStatusType.IKKE_AKTUELL),
            ),
            tiltakshistorikkKometTiltak(
                status = KometDeltakerStatusDto(type = KometDeltakerStatusDto.DeltakerStatusType.VURDERES),
            ),
            tiltakshistorikkKometTiltak(
                status = KometDeltakerStatusDto(type = KometDeltakerStatusDto.DeltakerStatusType.FEILREGISTRERT),
            ),
            tiltakshistorikkKometTiltak(
                status = KometDeltakerStatusDto(type = KometDeltakerStatusDto.DeltakerStatusType.PABEGYNT_REGISTRERING),
            ),
            tiltakshistorikkKometTiltak(
                status = KometDeltakerStatusDto(type = KometDeltakerStatusDto.DeltakerStatusType.SOKT_INN),
            ),
            tiltakshistorikkKometTiltak(
                status = KometDeltakerStatusDto(type = KometDeltakerStatusDto.DeltakerStatusType.VENTELISTE),
            ),
            tiltakshistorikkTeamTiltakTiltak(
                status = ArbeidsgiverAvtaleStatusDto.PAABEGYNT,
            ),
            tiltakshistorikkTeamTiltakTiltak(
                status = ArbeidsgiverAvtaleStatusDto.MANGLER_GODKJENNING,
            ),
            tiltakshistorikkTeamTiltakTiltak(
                status = ArbeidsgiverAvtaleStatusDto.ANNULLERT,
            ),
            tiltakshistorikkArenaTiltak(
                status = ArenaDeltakerStatusDto.AKTUELL,
            ),
            tiltakshistorikkArenaTiltak(
                status = ArenaDeltakerStatusDto.AVSLAG,
            ),
            tiltakshistorikkArenaTiltak(
                status = ArenaDeltakerStatusDto.GJENNOMFORING_AVLYST,
            ),
            tiltakshistorikkArenaTiltak(
                status = ArenaDeltakerStatusDto.IKKE_AKTUELL,
            ),
            tiltakshistorikkArenaTiltak(
                status = ArenaDeltakerStatusDto.INFORMASJONSMOTE,
            ),
            tiltakshistorikkArenaTiltak(
                status = ArenaDeltakerStatusDto.TAKKET_NEI_TIL_TILBUD,
            ),
            tiltakshistorikkArenaTiltak(
                status = ArenaDeltakerStatusDto.VENTELISTE,
            ),
            tiltakshistorikkArenaTiltak(
                status = ArenaDeltakerStatusDto.FEILREGISTRERT,
            ),
        )

        val tiltak = tiltakshistorikkService.hentTiltakshistorikkForSoknad(fnr)

        tiltak.size shouldBe 16
        tiltak.map {
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
    fun `tiltak fra arena med status GJENNOMFORES gir DELTAR hvis startdato har passert, ellers VENTER_PÅ_OPPSTART`() {
        val arenaTiltak1 = tiltakshistorikkArenaTiltak(
            tiltak = TiltakshistorikkV1Dto.ArenaDeltakelse.Tiltakstype(
                tiltakskode = "FORSAMOGRU",
                navn = "Forsøk AMO gruppe",
            ),
            status = ArenaDeltakerStatusDto.GJENNOMFORES,
            LocalDate.now().plusDays(10),
            LocalDate.now().plusDays(20),
        )
        val arenaTiltak2 = tiltakshistorikkArenaTiltak(
            tiltak = TiltakshistorikkV1Dto.ArenaDeltakelse.Tiltakstype(
                tiltakskode = "ETAB",
                navn = "Egenetablering",
            ),
            status = ArenaDeltakerStatusDto.GJENNOMFORES,
            LocalDate.now().minusDays(20),
            LocalDate.now().minusDays(10),
        )
        coEvery { tiltakshistorikkClient.hentTiltaksdeltakelser(any()) } returns listOf(arenaTiltak1, arenaTiltak2)

        tiltakshistorikkService.hentTiltakshistorikkForSaksbehandling(fnr).also { actual ->
            actual.size shouldBe 2
            actual[0].deltakelseStatus shouldBe VENTER_PA_OPPSTART
            actual[1].deltakelseStatus shouldBe DELTAR
        }

        tiltakshistorikkService.hentTiltakshistorikkForSoknad(fnr).also { actual ->
            // filtrerer bort ETAB som ikke gir rett
            actual.size shouldBe 1
            actual[0].deltakelseStatus shouldBe VENTER_PA_OPPSTART
        }
    }

    @Test
    fun `tiltak fra komet, arena og team tiltak som gir rett på tiltakspenger returnerer true`() {
        coEvery { tiltakshistorikkClient.hentTiltaksdeltakelser(any()) } returns listOf(
            tiltakshistorikkKometTiltak(
                tiltak = TiltakshistorikkV1Dto.TeamKometDeltakelse.Tiltakstype(
                    tiltakskode = TiltakskodeDto.ARBEIDSFORBEREDENDE_TRENING,
                    navn = "Arbeidsforberedende trening",
                ),
                status = KometDeltakerStatusDto(
                    type = KometDeltakerStatusDto.DeltakerStatusType.DELTAR,
                ),
            ),
            tiltakshistorikkArenaTiltak(
                status = ArenaDeltakerStatusDto.GJENNOMFORES,
            ),
            tiltakshistorikkTeamTiltakTiltak(
                status = ArbeidsgiverAvtaleStatusDto.GJENNOMFORES,
            ),
        )

        tiltakshistorikkService.hentTiltakshistorikkForSaksbehandling(fnr).also {
            it.size shouldBe 3
            it[0].gjennomforing.arenaKode.rettPåTiltakspenger shouldBe true
            it[1].gjennomforing.arenaKode.rettPåTiltakspenger shouldBe true
            it[2].gjennomforing.arenaKode.rettPåTiltakspenger shouldBe true
        }

        tiltakshistorikkService.hentTiltakshistorikkForSoknad(fnr).also {
            it.size shouldBe 3
            it[0].gjennomforing.arenaKode.rettPåTiltakspenger shouldBe true
            it[1].gjennomforing.arenaKode.rettPåTiltakspenger shouldBe true
            it[2].gjennomforing.arenaKode.rettPåTiltakspenger shouldBe true
        }
    }

    @Test
    fun `tiltak fra komet og arena som ikke gir rett på tiltakspenger returnerer false`() {
        coEvery { tiltakshistorikkClient.hentTiltaksdeltakelser(any()) } returns listOf(
            tiltakshistorikkKometTiltak(
                tiltak = TiltakshistorikkV1Dto.TeamKometDeltakelse.Tiltakstype(
                    tiltakskode = TiltakskodeDto.VARIG_TILRETTELAGT_ARBEID_SKJERMET,
                    navn = "Varig tilrettelagt arbeid",
                ),
                status = KometDeltakerStatusDto(
                    type = KometDeltakerStatusDto.DeltakerStatusType.DELTAR,
                ),
            ),
            tiltakshistorikkArenaTiltak(
                tiltak = TiltakshistorikkV1Dto.ArenaDeltakelse.Tiltakstype(
                    tiltakskode = "ETAB",
                    navn = "Egenetablering",
                ),
                status = ArenaDeltakerStatusDto.GJENNOMFORES,
            ),
            tiltakshistorikkTeamTiltakTiltak(
                tiltakstype = TiltakshistorikkV1Dto.TeamTiltakAvtale.Tiltakstype(
                    tiltakskode = TiltakshistorikkV1Dto.TeamTiltakAvtale.Tiltakskode.MENTOR,
                    navn = "Mentor",
                ),
                status = ArbeidsgiverAvtaleStatusDto.GJENNOMFORES,
            ),
        )

        tiltakshistorikkService.hentTiltakshistorikkForSaksbehandling(fnr).also {
            it.size shouldBe 3
            it.first { it.gjennomforing.arenaKode == TiltakResponsDTO.TiltakType.MENTOR }.gjennomforing.arenaKode.rettPåTiltakspenger shouldBe false
            it.first { it.gjennomforing.arenaKode == TiltakResponsDTO.TiltakType.VASV }.gjennomforing.arenaKode.rettPåTiltakspenger shouldBe false
            it.first { it.gjennomforing.arenaKode == TiltakResponsDTO.TiltakType.ETAB }.gjennomforing.arenaKode.rettPåTiltakspenger shouldBe false
        }

        tiltakshistorikkService.hentTiltakshistorikkForSoknad(fnr).also {
            it.size shouldBe 0
        }
    }

    @Test
    fun `tiltak med status som skal dukke opp i søknaden gir rett til å søke`() {
        coEvery { tiltakshistorikkClient.hentTiltaksdeltakelser(any()) } returns listOf(
            tiltakshistorikkKometTiltak(
                status = KometDeltakerStatusDto(type = KometDeltakerStatusDto.DeltakerStatusType.AVBRUTT),
            ),
            tiltakshistorikkKometTiltak(
                status = KometDeltakerStatusDto(type = KometDeltakerStatusDto.DeltakerStatusType.FULLFORT),
            ),
            tiltakshistorikkKometTiltak(
                status = KometDeltakerStatusDto(type = KometDeltakerStatusDto.DeltakerStatusType.DELTAR),
            ),
            tiltakshistorikkKometTiltak(
                status = KometDeltakerStatusDto(type = KometDeltakerStatusDto.DeltakerStatusType.VENTER_PA_OPPSTART),
            ),
            tiltakshistorikkKometTiltak(
                status = KometDeltakerStatusDto(type = KometDeltakerStatusDto.DeltakerStatusType.HAR_SLUTTET),
            ),
            tiltakshistorikkTeamTiltakTiltak(
                status = ArbeidsgiverAvtaleStatusDto.KLAR_FOR_OPPSTART,
            ),
            tiltakshistorikkTeamTiltakTiltak(
                status = ArbeidsgiverAvtaleStatusDto.GJENNOMFORES,
            ),
            tiltakshistorikkTeamTiltakTiltak(
                status = ArbeidsgiverAvtaleStatusDto.AVSLUTTET,
            ),
            tiltakshistorikkTeamTiltakTiltak(
                status = ArbeidsgiverAvtaleStatusDto.AVBRUTT,
            ),
            tiltakshistorikkArenaTiltak(
                status = ArenaDeltakerStatusDto.DELTAKELSE_AVBRUTT,
            ),
            tiltakshistorikkArenaTiltak(
                status = ArenaDeltakerStatusDto.FULLFORT,
            ),
            tiltakshistorikkArenaTiltak(
                status = ArenaDeltakerStatusDto.GJENNOMFORES,
            ),
            tiltakshistorikkArenaTiltak(
                status = ArenaDeltakerStatusDto.GJENNOMFORING_AVBRUTT,
            ),
            tiltakshistorikkArenaTiltak(
                status = ArenaDeltakerStatusDto.IKKE_MOTT,
            ),
            tiltakshistorikkArenaTiltak(
                status = ArenaDeltakerStatusDto.TAKKET_JA_TIL_TILBUD,
            ),
            tiltakshistorikkArenaTiltak(
                status = ArenaDeltakerStatusDto.TILBUD,
            ),
        )

        tiltakshistorikkService.hentTiltakshistorikkForSaksbehandling(fnr).also {
            it.size shouldBe 16
            it.all { it.deltakelseStatus.rettTilÅSøke }
            it.all { it.gjennomforing.arenaKode.rettPåTiltakspenger }
        }

        tiltakshistorikkService.hentTiltakshistorikkForSoknad(fnr).also {
            it.size shouldBe 16
            it.all { it.deltakelseStatus.rettTilÅSøke }
            it.all { it.gjennomforing.arenaKode.rettPåTiltakspenger }
        }
    }

    @Test
    fun `tiltak med status som ikke skal dukke opp i søknaden gir ikke rett til å søke`() {
        coEvery { tiltakshistorikkClient.hentTiltaksdeltakelser(any()) } returns listOf(
            tiltakshistorikkKometTiltak(
                status = KometDeltakerStatusDto(type = KometDeltakerStatusDto.DeltakerStatusType.IKKE_AKTUELL),
            ),
            tiltakshistorikkKometTiltak(
                status = KometDeltakerStatusDto(type = KometDeltakerStatusDto.DeltakerStatusType.VURDERES),
            ),
            tiltakshistorikkKometTiltak(
                status = KometDeltakerStatusDto(type = KometDeltakerStatusDto.DeltakerStatusType.FEILREGISTRERT),
            ),
            tiltakshistorikkKometTiltak(
                status = KometDeltakerStatusDto(type = KometDeltakerStatusDto.DeltakerStatusType.PABEGYNT_REGISTRERING),
            ),
            tiltakshistorikkKometTiltak(
                status = KometDeltakerStatusDto(type = KometDeltakerStatusDto.DeltakerStatusType.SOKT_INN),
            ),
            tiltakshistorikkKometTiltak(
                status = KometDeltakerStatusDto(type = KometDeltakerStatusDto.DeltakerStatusType.VENTELISTE),
            ),
            tiltakshistorikkTeamTiltakTiltak(
                status = ArbeidsgiverAvtaleStatusDto.PAABEGYNT,
            ),
            tiltakshistorikkTeamTiltakTiltak(
                status = ArbeidsgiverAvtaleStatusDto.MANGLER_GODKJENNING,
            ),
            tiltakshistorikkTeamTiltakTiltak(
                status = ArbeidsgiverAvtaleStatusDto.ANNULLERT,
            ),
            tiltakshistorikkArenaTiltak(
                status = ArenaDeltakerStatusDto.AKTUELL,
            ),
            tiltakshistorikkArenaTiltak(
                status = ArenaDeltakerStatusDto.AVSLAG,
            ),
            tiltakshistorikkArenaTiltak(
                status = ArenaDeltakerStatusDto.GJENNOMFORING_AVLYST,
            ),
            tiltakshistorikkArenaTiltak(
                status = ArenaDeltakerStatusDto.IKKE_AKTUELL,
            ),
            tiltakshistorikkArenaTiltak(
                status = ArenaDeltakerStatusDto.INFORMASJONSMOTE,
            ),
            tiltakshistorikkArenaTiltak(
                status = ArenaDeltakerStatusDto.TAKKET_NEI_TIL_TILBUD,
            ),
            tiltakshistorikkArenaTiltak(
                status = ArenaDeltakerStatusDto.VENTELISTE,
            ),
            tiltakshistorikkArenaTiltak(
                status = ArenaDeltakerStatusDto.FEILREGISTRERT,
            ),
        )

        tiltakshistorikkService.hentTiltakshistorikkForSaksbehandling(fnr).also {
            it.size shouldBe 17
            it.all { !it.deltakelseStatus.rettTilÅSøke }
        }

        tiltakshistorikkService.hentTiltakshistorikkForSoknad(fnr).also {
            it.size shouldBe 0
        }
    }

    @Test
    fun `filtrerer ikke bort tiltak som mangler datoer`() {
        coEvery { tiltakshistorikkClient.hentTiltaksdeltakelser(any()) } returns listOf(
            tiltakshistorikkKometTiltak(
                status = KometDeltakerStatusDto(type = KometDeltakerStatusDto.DeltakerStatusType.DELTAR),
            ),
            tiltakshistorikkKometTiltak(
                status = KometDeltakerStatusDto(type = KometDeltakerStatusDto.DeltakerStatusType.DELTAR),
                fom = null,
            ),
            tiltakshistorikkKometTiltak(
                status = KometDeltakerStatusDto(type = KometDeltakerStatusDto.DeltakerStatusType.DELTAR),
                tom = null,
            ),
            tiltakshistorikkKometTiltak(
                status = KometDeltakerStatusDto(type = KometDeltakerStatusDto.DeltakerStatusType.DELTAR),
                fom = null,
                tom = null,
            ),
        )

        tiltakshistorikkService.hentTiltakshistorikkForSaksbehandling(fnr).also {
            it.size shouldBe 4
        }

        tiltakshistorikkService.hentTiltakshistorikkForSoknad(fnr).also {
            it.size shouldBe 4
        }
    }

    @Test
    fun `tiltak med til og med dato satt etter fra og med dato filtreres bort`() {
        coEvery { tiltakshistorikkClient.hentTiltaksdeltakelser(any()) } returns listOf(
            tiltakshistorikkKometTiltak(
                status = KometDeltakerStatusDto(type = KometDeltakerStatusDto.DeltakerStatusType.AVBRUTT),
            ),
            tiltakshistorikkKometTiltak(
                status = KometDeltakerStatusDto(type = KometDeltakerStatusDto.DeltakerStatusType.AVBRUTT),
                fom = LocalDate.now(),
                tom = LocalDate.now().minusDays(1),
            ),
        )

        tiltakshistorikkService.hentTiltakshistorikkForSaksbehandling(fnr).also {
            it.size shouldBe 1
        }

        tiltakshistorikkService.hentTiltakshistorikkForSoknad(fnr).also {
            it.size shouldBe 1
        }
    }

    private fun getRespons(): TiltakshistorikkV1Response {
        val respons = """
            {
              "historikk": [
                {
                  "type": "ArenaDeltakelse",
                  "norskIdent": "12345678910",
                  "startDato": "2024-07-03",
                  "sluttDato": "2024-10-31",
                  "id": "ddb13a2b-cd65-432d-965c-9167938a26a4",
                  "tittel": "Arbeidsmarkedsopplæring (enkeltplass) hos Arrangør",
                  "arenaId": 1234567,
                  "status": "GJENNOMFORES",
                  "tiltakstype": {
                    "tiltakskode": "ENKELAMO",
                    "navn": "Arbeidsmarkedsopplæring (enkeltplass)"
                  },
                  "gjennomforing": {
                    "id": "702ab5bd-5a6f-4c0e-96d9-975574af9adb",
                    "navn": "Enkel-AMO hos Arrangør",
                    "deltidsprosent": 100.0
                  },
                  "arrangor": {
                    "hovedenhet": null,
                    "underenhet": {
                      "organisasjonsnummer": "987654321",
                      "navn": "Arrangør"
                    }
                  },
                  "deltidsprosent": 100.0,
                  "dagerPerUke": 5.0,
                  "opphav": "ARENA"
                },
                {
                  "type": "TeamKometDeltakelse",
                  "norskIdent": "12345678910",
                  "startDato": "2024-04-04",
                  "sluttDato": "2024-04-05",
                  "id": "6d54228f-534f-4b4b-9160-65eae26a3b06",
                  "tittel": "Arbeidsforberedende trening hos Arrangør",
                  "status": {
                    "type": "HAR_SLUTTET",
                    "aarsak": "SYK",
                    "opprettetDato": "2024-04-04T14:32:32.003702"
                  },
                  "tiltakstype": {
                    "tiltakskode": "ARBEIDSFORBEREDENDE_TRENING",
                    "navn": "Arbeidsforberedende trening"
                  },
                  "gjennomforing": {
                    "id": "9caf398e-8e38-41fc-af29-b7ee6f62205a",
                    "navn": "Testgjennomføring",
                    "deltidsprosent": 100
                  },
                  "arrangor": {
                    "hovedenhet": null,
                    "underenhet": {
                    "organisasjonsnummer": "876543210",
                    "navn": "Arrangør"
                    }
                  },
                  "deltidsprosent": 60.0,
                  "dagerPerUke": 3.0,
                  "opphav": "TEAM_KOMET"
                },
                {
                  "type": "TeamTiltakAvtale",
                  "norskIdent": "12345678910",
                  "startDato": "2024-01-01",
                  "sluttDato": "2024-12-31",
                  "id": "9dea48c1-d494-4664-9427-bdb20a6f265f",
                  "tittel": "Arbeidstrening hos Arbeidsgiver",
                  "tiltakstype": {
                    "tiltakskode": "ARBEIDSTRENING",
                    "navn": "Arbeidstrening"
                  },
                  "status": "GJENNOMFORES",
                  "stillingsprosent": 100,
                  "dagerPerUke": 5,
                  "arbeidsgiver": {
                    "organisasjonsnummer": "876543210",
                    "navn": "Arbeidsgiver"
                  },
                  "opphav": "TEAM_TILTAK"
                }
              ],
              "meldinger": []
            }
        """.trimIndent()

        return objectMapper.readValue<TiltakshistorikkV1Response>(respons)
    }
}

fun tiltakshistorikkArenaTiltak(
    tiltak: TiltakshistorikkV1Dto.ArenaDeltakelse.Tiltakstype = TiltakshistorikkV1Dto.ArenaDeltakelse.Tiltakstype(
        tiltakskode = TiltakResponsDTO.TiltakType.ENKELAMO.name,
        navn = TiltakResponsDTO.TiltakType.ENKELAMO.navn,
    ),
    status: ArenaDeltakerStatusDto,
    fom: LocalDate? = LocalDate.of(2023, 1, 1),
    tom: LocalDate? = LocalDate.of(2023, 3, 31),
) = TiltakshistorikkV1Dto.ArenaDeltakelse(
    startDato = fom,
    sluttDato = tom,
    id = UUID.randomUUID(),
    tittel = "Tiltak hos arrangør",
    arenaId = 1234567,
    status = status,
    tiltakstype = tiltak,
    gjennomforing = TiltakshistorikkV1Dto.Gjennomforing(
        id = UUID.randomUUID(),
        deltidsprosent = null,
    ),
    arrangor = TiltakshistorikkV1Dto.Arrangor(
        hovedenhet = null,
        underenhet = TiltakshistorikkV1Dto.Virksomhet(
            navn = "Arrangør",
        ),
    ),
    deltidsprosent = 100.0f,
    dagerPerUke = 5.0f,
)

fun tiltakshistorikkKometTiltak(
    tiltak: TiltakshistorikkV1Dto.TeamKometDeltakelse.Tiltakstype = TiltakshistorikkV1Dto.TeamKometDeltakelse.Tiltakstype(
        tiltakskode = TiltakskodeDto.ARBEIDSFORBEREDENDE_TRENING,
        navn = "Arbeidsforberedende trening",
    ),
    status: KometDeltakerStatusDto,
    fom: LocalDate? = LocalDate.of(2023, 1, 1),
    tom: LocalDate? = LocalDate.of(2023, 3, 31),
) = TiltakshistorikkV1Dto.TeamKometDeltakelse(
    startDato = fom,
    sluttDato = tom,
    id = UUID.randomUUID(),
    tittel = "Tiltak hos arrangør",
    status = status,
    tiltakstype = tiltak,
    gjennomforing = TiltakshistorikkV1Dto.Gjennomforing(
        id = UUID.randomUUID(),
        deltidsprosent = 100.0f,
    ),
    arrangor = TiltakshistorikkV1Dto.Arrangor(
        hovedenhet = null,
        underenhet = TiltakshistorikkV1Dto.Virksomhet(
            navn = "Arrangør",
        ),
    ),
    deltidsprosent = 100.0f,
    dagerPerUke = 5.0f,
)

fun tiltakshistorikkTeamTiltakTiltak(
    tiltakstype: TiltakshistorikkV1Dto.TeamTiltakAvtale.Tiltakstype = TiltakshistorikkV1Dto.TeamTiltakAvtale.Tiltakstype(
        tiltakskode = TiltakshistorikkV1Dto.TeamTiltakAvtale.Tiltakskode.ARBEIDSTRENING,
        navn = "Arbeidstrening",
    ),
    status: ArbeidsgiverAvtaleStatusDto,
    fom: LocalDate? = LocalDate.of(2023, 1, 1),
    tom: LocalDate? = LocalDate.of(2023, 3, 31),
) = TiltakshistorikkV1Dto.TeamTiltakAvtale(
    startDato = fom,
    sluttDato = tom,
    id = UUID.randomUUID(),
    tittel = "Tiltak hos arbeidsgiver",
    tiltakstype = tiltakstype,
    status = status,
    stillingsprosent = 100.0f,
    dagerPerUke = 5.0f,
    arbeidsgiver = TiltakshistorikkV1Dto.Virksomhet(
        navn = "Arbeidsgiver",
    ),
)
