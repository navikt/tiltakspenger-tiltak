package no.nav.tiltakspenger.tiltak.testdata

import java.time.LocalDate
import java.util.UUID

data class OpprettTestDeltakelseRequest(
    val personident: String,
    val deltakerlisteId: UUID,
    val startdato: LocalDate,
    val deltakelsesprosent: Int,
    val dagerPerUke: Int?,
)
