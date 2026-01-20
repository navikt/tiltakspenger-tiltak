package no.nav.tiltakspenger.tiltak.routes

import no.nav.tiltakspenger.libs.tiltak.TiltakResponsDTO.DeltakerStatusDTO
import no.nav.tiltakspenger.libs.tiltak.TiltakResponsDTO.TiltakType
import java.time.LocalDate

// Skal flyttes til libs
data class TiltakshistorikkDTO(
    val id: String,
    val gjennomforing: GjennomforingDTO,
    val deltakelseFom: LocalDate?,
    val deltakelseTom: LocalDate?,
    val deltakelseStatus: DeltakerStatusDTO,
    val deltakelsePerUke: Float?,
    val deltakelseProsent: Float?,
    val kilde: Kilde,
) {
    data class GjennomforingDTO(
        val id: String,
        val visningsnavn: String,
        val arrangornavn: String?,
        val typeNavn: String,
        val arenaKode: TiltakType,
        val deltidsprosent: Double?,
    )

    enum class Kilde {
        ARENA,
        KOMET,
        TEAM_TILTAK,
    }
}
