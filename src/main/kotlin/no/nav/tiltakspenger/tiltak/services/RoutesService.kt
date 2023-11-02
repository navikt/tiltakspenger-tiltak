package no.nav.tiltakspenger.tiltak.services

import no.nav.tiltakspenger.libs.tiltak.TiltakResponsDTO.TiltakDTO

interface RoutesService {
    fun hentAlleTiltak(fnr: String): List<TiltakDTO>
    fun hentTiltakForSøknad(fnr: String): List<TiltakDTO>
}
