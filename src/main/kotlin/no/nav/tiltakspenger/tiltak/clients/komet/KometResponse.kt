package no.nav.tiltakspenger.tiltak.clients.komet

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import kotlin.collections.List

data class KometResponse(
    val deltakelser: List<DeltakerDTO>,
)

data class DeltakerDTO(
    val id: UUID,
    val gjennomforing: GjennomforingDTO,
    val startDato: LocalDate?,
    val sluttDato: LocalDate?,
    val status: DeltakerStatusDTO,
    val dagerPerUke: Float?,
    val prosentStilling: Float?,
    val registrertDato: LocalDateTime,
)

data class ArrangorDTO(
    val virksomhetsnummer: String,
    val navn: String,
)

data class GjennomforingDTO(
    val id: UUID,
    val navn: String,
    val type: String, // Arena type
    val arrangor: ArrangorDTO,
)

enum class DeltakerStatusDTO {
    VENTER_PA_OPPSTART, DELTAR, HAR_SLUTTET, IKKE_AKTUELL, VURDERES, AVBRUTT
}
