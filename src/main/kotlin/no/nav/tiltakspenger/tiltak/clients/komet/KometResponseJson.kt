package no.nav.tiltakspenger.tiltak.clients.komet

import no.nav.tiltakspenger.libs.tiltak.TiltakResponsDTO.DeltakerStatusDTO
import no.nav.tiltakspenger.libs.tiltak.TiltakResponsDTO.GjennomføringDTO
import no.nav.tiltakspenger.libs.tiltak.TiltakResponsDTO.TiltakDTO
import no.nav.tiltakspenger.libs.tiltak.TiltakResponsDTO.TiltakType
import no.nav.tiltakspenger.libs.tiltak.TiltakTilSaksbehandlingDTO
import no.nav.tiltakspenger.tiltak.services.earliest
import no.nav.tiltakspenger.tiltak.services.latest
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * https://confluence.adeo.no/pages/viewpage.action?pageId=573710206
 * https://confluence.adeo.no/pages/viewpage.action?pageId=597205082
 */
data class KometResponseJson(
    val id: String,
    val gjennomforing: GjennomforingDTO,
    val startDato: LocalDate?,
    val sluttDato: LocalDate?,
    val status: String,
    val dagerPerUke: Float?,
    val prosentStilling: Float?,
    val registrertDato: LocalDateTime,
) {
    data class GjennomforingDTO(
        val id: String,
        val navn: String,
        // Arena type
        val type: String,
        val tiltakstypeNavn: String,
        val arrangor: ArrangorDTO,
    ) {
        data class ArrangorDTO(
            val virksomhetsnummer: String,
            val navn: String,
        )
    }
}

internal fun KometResponseJson.toSaksbehandlingDTO(): TiltakTilSaksbehandlingDTO {
    return TiltakTilSaksbehandlingDTO(
        id = id,
        deltakelseFom = startDato,
        deltakelseTom = sluttDato,
        gjennomføringId = gjennomforing.id,
        typeNavn = gjennomforing.navn,
        typeKode = TiltakType.valueOf(gjennomforing.type),
        deltakelseStatus = this.status.toDeltakerStatusDTO(),
        deltakelsePerUke = dagerPerUke,
        deltakelseProsent = prosentStilling,
        kilde = "Komet",
    )
}

internal fun KometResponseJson.toSøknadTiltak(): TiltakDTO {
    return TiltakDTO(
        id = id,
        deltakelseFom = earliest(startDato, sluttDato),
        deltakelseTom = latest(startDato, sluttDato),
        deltakelseDagerUke = dagerPerUke,
        deltakelseProsent = prosentStilling,
        registrertDato = registrertDato,
        gjennomforing = GjennomføringDTO(
            id = gjennomforing.id,
            arrangørnavn = gjennomforing.arrangor.navn,
            typeNavn = gjennomforing.tiltakstypeNavn,
            arenaKode = TiltakType.valueOf(gjennomforing.type),
        ),
        kilde = "Komet",
        deltakelseStatus = this.status.toDeltakerStatusDTO(),
    )
}

private fun String.toDeltakerStatusDTO() = when (this) {
    "AVBRUTT" -> DeltakerStatusDTO.AVBRUTT
    "FULLFORT" -> DeltakerStatusDTO.FULLFORT
    "DELTAR" -> DeltakerStatusDTO.DELTAR
    "IKKE_AKTUELL" -> DeltakerStatusDTO.IKKE_AKTUELL
    "VENTER_PA_OPPSTART" -> DeltakerStatusDTO.VENTER_PA_OPPSTART
    "HAR_SLUTTET" -> DeltakerStatusDTO.HAR_SLUTTET

    // Disse er ikke med i søknaden
    "VURDERES" -> DeltakerStatusDTO.VURDERES
    "FEILREGISTRERT" -> DeltakerStatusDTO.FEILREGISTRERT
    "PABEGYNT_REGISTRERING" -> DeltakerStatusDTO.PABEGYNT_REGISTRERING
    "SOKT_INN" -> DeltakerStatusDTO.SOKT_INN
    "VENTELISTE" -> DeltakerStatusDTO.VENTELISTE
    /** Mappes til AKTUELL i Arena */
    "UTKAST_TIL_PAMELDING" -> DeltakerStatusDTO.PABEGYNT_REGISTRERING
    /** Mappes til IKKAKTUELL i Arena */
    "AVBRUTT_UTKAST" -> DeltakerStatusDTO.IKKE_AKTUELL
    else -> {
        throw RuntimeException("Klarte ikke tolke respons fra Komet. Ukjent deltakerstatus: $this")
    }
}
