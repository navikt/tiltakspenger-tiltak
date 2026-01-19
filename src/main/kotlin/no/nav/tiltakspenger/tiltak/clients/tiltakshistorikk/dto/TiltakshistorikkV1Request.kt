package no.nav.tiltakspenger.tiltak.clients.tiltakshistorikk.dto

data class TiltakshistorikkV1Request(
    val identer: List<NorskIdent>,
    val maxAgeYears: Int? = null,
)
