package no.nav.tiltakspenger.tiltak.services

import no.nav.tiltakspenger.libs.arena.tiltak.ArenaTiltaksaktivitetResponsDTO.DeltakelsesPeriodeDTO
import no.nav.tiltakspenger.libs.arena.tiltak.ArenaTiltaksaktivitetResponsDTO.DeltakerStatusType
import no.nav.tiltakspenger.libs.arena.tiltak.ArenaTiltaksaktivitetResponsDTO.TiltaksaktivitetDTO
import no.nav.tiltakspenger.libs.tiltak.TiltakResponsDTO.DeltakerStatusDTO
import no.nav.tiltakspenger.libs.tiltak.TiltakResponsDTO.GjennomføringDTO
import no.nav.tiltakspenger.libs.tiltak.TiltakResponsDTO.TiltakDTO
import no.nav.tiltakspenger.libs.tiltak.TiltakResponsDTO.TiltakType
import no.nav.tiltakspenger.libs.tiltak.TiltakTilSaksbehandlingDTO
import java.time.LocalDate
import java.time.LocalDateTime

// Vi får ikke gjennomføringId fra Arena
// confluenseside for endepunktet vi kaller for å hente Arenatiltak: https://confluence.adeo.no/pages/viewpage.action?pageId=470748287
internal fun TiltaksaktivitetDTO.toSaksbehandlingDTO(): TiltakTilSaksbehandlingDTO = TiltakTilSaksbehandlingDTO(
    // TODO pre-mvp jah: Dette er tiltaksdeltakerId, jeg er litt usikker på om det er riktig nok at denne mappes til id.
    id = aktivitetId,
    gjennomføringId = null,
    deltakelseFom = earliest(deltakelsePeriode?.fom, deltakelsePeriode?.tom),
    deltakelseTom = latest(deltakelsePeriode?.fom, deltakelsePeriode?.tom),
    typeNavn = tiltakType.navn,
    typeKode = TiltakType.valueOf(tiltakType.name),
    deltakelseStatus = deltakerStatusType.toDTO(deltakelsePeriode),
    deltakelsePerUke = antallDagerPerUke,
    deltakelseProsent = deltakelseProsent,
    kilde = "Arena",
)

internal fun TiltaksaktivitetDTO.toSøknadTiltak(): TiltakDTO =
    TiltakDTO(
        id = aktivitetId,
        gjennomforing = GjennomføringDTO(
            id = "",
            arrangørnavn = arrangoer ?: "Ukjent",
            typeNavn = tiltakType.navn,
            arenaKode = TiltakType.valueOf(tiltakType.name),
        ),
        deltakelseFom = earliest(deltakelsePeriode?.fom, deltakelsePeriode?.tom),
        deltakelseTom = latest(deltakelsePeriode?.fom, deltakelsePeriode?.tom),
        deltakelseStatus = deltakerStatusType.toDTO(deltakelsePeriode),
        deltakelseDagerUke = antallDagerPerUke,
        deltakelseProsent = deltakelseProsent,
        kilde = "Arena",
        registrertDato = statusSistEndret?.let { LocalDateTime.from(it.atStartOfDay()) } ?: LocalDateTime.now(),
    )

// Fordi Arena noen ganger bytter om på fom og tom, må vi bytte tilbake hvis det skjer...
internal fun earliest(fom: LocalDate?, tom: LocalDate?) =
    when {
        fom != null && tom != null -> if (tom.isBefore(fom)) {
            securelog.warn { "fom er etter tom, så vi bytter om de to datoene på tiltaket" }
            tom
        } else {
            fom
        }

        else -> fom
    }

internal fun latest(fom: LocalDate?, tom: LocalDate?) =
    when {
        fom != null && tom != null -> if (fom.isAfter(tom)) fom else tom
        else -> tom
    }

fun DeltakerStatusType.toDTO(deltakelsePeriode: DeltakelsesPeriodeDTO?): DeltakerStatusDTO {
    val fom = earliest(deltakelsePeriode?.fom, deltakelsePeriode?.tom) ?: LocalDate.MAX
    val startDatoErFremITid = (fom.isAfter(LocalDate.now()))

    return when (this) {
        DeltakerStatusType.DELAVB -> DeltakerStatusDTO.AVBRUTT
        DeltakerStatusType.FULLF -> DeltakerStatusDTO.FULLFORT
        DeltakerStatusType.GJENN -> if (startDatoErFremITid) DeltakerStatusDTO.VENTER_PA_OPPSTART else DeltakerStatusDTO.DELTAR
        DeltakerStatusType.GJENN_AVB -> DeltakerStatusDTO.AVBRUTT
        DeltakerStatusType.IKKEM -> DeltakerStatusDTO.AVBRUTT
        DeltakerStatusType.JATAKK -> DeltakerStatusDTO.DELTAR
        DeltakerStatusType.TILBUD -> if (startDatoErFremITid) DeltakerStatusDTO.VENTER_PA_OPPSTART else DeltakerStatusDTO.DELTAR

        // Disse er ikke med i søknaden
        DeltakerStatusType.AKTUELL -> DeltakerStatusDTO.SOKT_INN
        DeltakerStatusType.AVSLAG -> DeltakerStatusDTO.IKKE_AKTUELL
        DeltakerStatusType.GJENN_AVL -> DeltakerStatusDTO.IKKE_AKTUELL
        DeltakerStatusType.IKKAKTUELL -> DeltakerStatusDTO.IKKE_AKTUELL
        DeltakerStatusType.INFOMOETE -> DeltakerStatusDTO.VENTELISTE
        DeltakerStatusType.NEITAKK -> DeltakerStatusDTO.IKKE_AKTUELL
        DeltakerStatusType.VENTELISTE -> DeltakerStatusDTO.VENTELISTE
        DeltakerStatusType.FEILREG -> DeltakerStatusDTO.FEILREGISTRERT
    }
}

val tiltakViFårFraKomet = setOf(
    "INDOPPFAG",
    "ARBFORB",
    "AVKLARAG",
    "VASV",
    "ARBRRHDAG",
    "DIGIOPPARB",
    "JOBBK",
    "GRUPPEAMO",
    "GRUFAGYRKE",
)
