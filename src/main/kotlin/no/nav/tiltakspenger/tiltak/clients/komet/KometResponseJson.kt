package no.nav.tiltakspenger.tiltak.clients.komet

import no.nav.tiltakspenger.libs.tiltak.KometDeltakerStatusType
import no.nav.tiltakspenger.libs.tiltak.TiltakResponsDTO.GjennomføringDTO
import no.nav.tiltakspenger.libs.tiltak.TiltakResponsDTO.TiltakDTO
import no.nav.tiltakspenger.libs.tiltak.TiltakResponsDTO.TiltakType
import no.nav.tiltakspenger.libs.tiltak.TiltakTilSaksbehandlingDTO
import no.nav.tiltakspenger.libs.tiltak.toDeltakerStatusDTO
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * https://confluence.adeo.no/pages/viewpage.action?pageId=573710206
 * https://confluence.adeo.no/pages/viewpage.action?pageId=597205082
 * Formatet er mye det samme som beskrevet her: https://github.com/navikt/amt-tiltak/blob/main/.docs/deltaker-v1.md
 * https://github.com/navikt/amt-deltaker/blob/7b442ad70573ca4d19ff4947bf56f2610c398861/src/main/kotlin/no/nav/amt/deltaker/external/api/SystemApi.kt
 *
 */
data class KometResponseJson(
    val id: String,
    val gjennomforing: GjennomforingDTO,
    val startDato: LocalDate?,
    val sluttDato: LocalDate?,
    val status: KometDeltakerStatusType,
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
        gjennomforing = GjennomføringDTO(
            id = gjennomforing.id,
            arrangørnavn = gjennomforing.arrangor.navn,
            typeNavn = gjennomforing.tiltakstypeNavn,
            arenaKode = TiltakType.valueOf(gjennomforing.type),
        ),
        deltakelseFom = startDato,
        deltakelseTom = sluttDato,
        gjennomføringId = gjennomforing.id,
        typeNavn = gjennomforing.tiltakstypeNavn,
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
        deltakelseFom = startDato,
        deltakelseTom = sluttDato,
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
