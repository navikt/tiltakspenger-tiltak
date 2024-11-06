package no.nav.tiltakspenger.tiltak.services

import no.nav.tiltakspenger.libs.tiltak.TiltakResponsDTO.TiltakDTO
import no.nav.tiltakspenger.libs.tiltak.TiltakTilSaksbehandlingDTO

interface RoutesService {
    fun hentTiltakForSaksbehandling(fnr: String, correlationId: String?): List<TiltakTilSaksbehandlingDTO>
    fun hentTiltakForSøknad(fnr: String, correlationId: String?): List<TiltakDTO>
}
