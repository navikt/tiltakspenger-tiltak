package no.nav.tiltakspenger.tiltak.testutils

import no.nav.tiltakspenger.libs.tiltak.TiltakResponsDTO
import no.nav.tiltakspenger.tiltak.clients.tiltakshistorikk.dto.ArbeidsgiverAvtaleStatusDto
import no.nav.tiltakspenger.tiltak.clients.tiltakshistorikk.dto.ArenaDeltakerStatusDto
import no.nav.tiltakspenger.tiltak.clients.tiltakshistorikk.dto.KometDeltakerStatusDto
import no.nav.tiltakspenger.tiltak.clients.tiltakshistorikk.dto.TiltakshistorikkV1Dto
import no.nav.tiltakspenger.tiltak.clients.tiltakshistorikk.dto.TiltakskodeDto
import java.time.LocalDate
import java.util.UUID

/**
 * Byggere for tiltaksdeltakelser i tester.
 * Rene funksjoner uten tilstand, så de kan kalles fritt fra parallelle tester; hver deltakelse får sin egen [UUID].
 */
internal fun tiltakshistorikkArenaTiltak(
    tiltak: TiltakshistorikkV1Dto.ArenaDeltakelse.Tiltakstype = TiltakshistorikkV1Dto.ArenaDeltakelse.Tiltakstype(
        tiltakskode = TiltakResponsDTO.TiltakTypeDTO.ENKELAMO.name,
        navn = TiltakResponsDTO.TiltakTypeDTO.ENKELAMO.navn,
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

internal fun tiltakshistorikkKometTiltak(
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

internal fun tiltakshistorikkTeamTiltakTiltak(
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
