package no.nav.tiltakspenger.tiltak.clients.arena

import java.time.LocalDate

data class ArenaDTO(
    val tiltaksaktiviteter: List<TiltaksaktivitetDTO>?,
    val feil: String,
) {
    data class TiltaksaktivitetDTO(
        val tiltakType: String,
        val aktivitetId: String,
        val tiltakLokaltNavn: String?,
        val arrangoer: String?,
        val bedriftsnummer: String?,
        val deltakelsePeriode: DeltakelsesPeriodeDTO?,
        val deltakelseProsent: Float?,
        val deltakerStatusType: String,
        val statusSistEndret: LocalDate?,
        val begrunnelseInnsoeking: String?,
        val antallDagerPerUke: Float?,
    )

    data class DeltakelsesPeriodeDTO(
        val fom: LocalDate?,
        val tom: LocalDate?,
    )
}
