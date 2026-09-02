package no.nav.tiltakspenger.tiltak.testutils

import no.nav.tiltakspenger.libs.common.FnrGenerator
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.httpklient.infra.transport.FakeHttpTransport
import no.nav.tiltakspenger.tiltak.clients.tiltakshistorikk.TiltakshistorikkClient
import no.nav.tiltakspenger.tiltak.clients.tiltakshistorikk.dto.TiltakshistorikkV1Dto
import no.nav.tiltakspenger.tiltak.clients.tiltakshistorikk.dto.TiltakshistorikkV1Response
import no.nav.tiltakspenger.tiltak.person.infra.http.pdl.PdlClient
import no.nav.tiltakspenger.tiltak.services.TiltakshistorikkService

/** Trådsikker, delt på tvers av tester: hvert kall gir et nytt, unikt syntetisk fnr. */
private val fnrGenerator = FnrGenerator()

/** Nytt, unikt syntetisk fødselsnummer til bruk i tester som ikke bryr seg om selve verdien. */
fun genererFnr(): String = fnrGenerator.generer().verdi

/**
 * Én komplett, isolert app-kontekst: ekte klienter og service over hver sin [FakeHttpTransport].
 *
 * Konteksten bygges friskt inne i hver test og deles aldri mellom tester — verken som instansfelt eller companion-verdi.
 * Det er bevisst: køene og opptakene i transportene er muterbar tilstand, og delt muterbar tilstand gjør testene avhengige av rekkefølge og umulige å kjøre parallelt.
 * Testene kjører parallelt (se `build.gradle.kts`), så et delt felt her ville gitt flakiness i stedet for en tydelig feil.
 */
class TiltakTestkontekst(
    /** Fødselsnummeret testen bruker; ligger her slik at både køing og assertions kan lene seg på samme verdi. */
    val fnr: String = genererFnr(),
) {
    val pdlTransport = FakeHttpTransport()
    val tiltakshistorikkTransport = FakeHttpTransport()

    val tiltakshistorikkService = TiltakshistorikkService(
        tiltakshistorikkClient = TiltakshistorikkClient(
            baseUrl = TILTAKSHISTORIKK_BASE_URL,
            clock = fixedClock,
            authTokenProvider = testTokenProvider,
            transport = tiltakshistorikkTransport,
        ),
        pdlClient = PdlClient(
            baseUrl = PDL_BASE_URL,
            clock = fixedClock,
            authTokenProvider = testTokenProvider,
            transport = pdlTransport,
        ),
        clock = fixedClock,
    )

    /**
     * Køer svarene for **ett** oppslag: først PDL-identene, så tiltaksdeltakelsene.
     * Kaller testen servicen flere ganger, må den kalle denne like mange ganger — køen er FIFO og konsumeres ett svar per HTTP-forsøk.
     */
    fun køOppslag(
        deltakelser: List<TiltakshistorikkV1Dto> = emptyList(),
        identer: List<String> = listOf(fnr),
    ) {
        køPdlIdenter(identer)
        køTiltaksdeltakelser(deltakelser)
    }

    fun køPdlIdenter(identer: List<String>) {
        pdlTransport.leggIKøJson(
            """{"data": {"hentIdenter": {"identer": [${identer.joinToString { """{"ident": "$it"}""" }}]}}, "errors": null}""",
        )
    }

    fun køTiltaksdeltakelser(deltakelser: List<TiltakshistorikkV1Dto>) {
        tiltakshistorikkTransport.leggIKøJson(TiltakshistorikkV1Response(historikk = deltakelser))
    }

    companion object {
        const val PDL_BASE_URL = "http://pdl.test"
        const val TILTAKSHISTORIKK_BASE_URL = "http://tiltakshistorikk.test"
    }
}
