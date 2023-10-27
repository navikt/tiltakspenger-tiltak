// package no.nav.tiltakspenger.services
//
// import io.kotest.matchers.collections.shouldContain
// import io.mockk.mockk
// import no.nav.tiltakspenger.tiltak.services.ArrangorResponseDTO
// import no.nav.tiltakspenger.tiltak.services.DeltakerStatusResponseDTO
// import no.nav.tiltakspenger.tiltak.services.GjennomforingResponseDTO
// import no.nav.tiltakspenger.tiltak.services.RouteServiceImpl
// import no.nav.tiltakspenger.tiltak.services.TiltakDeltakelseResponse
// import org.junit.jupiter.api.Test
// import java.time.LocalDate
// import java.time.LocalDateTime
// import java.util.*
//
// internal class RouteServiceImplTest {
//
//    @Test
//    fun `test service`() {
//        // TODO Denne er wip. Bare såvidt startet på.
//        //      Denne skal mocke kometClient og valpClient og sjekke at servicen svarer med forventet resultat
//        val fnr = "1234"
//        val service = RouteServiceImpl(
//            kometClient = mockk(),
//            valpClient = mockk(),
//        )
//        val response = service.hentTiltak(fnr)
//        response shouldContain TiltakDeltakelseResponse(
//            id = UUID.fromString(""),
//            gjennomforing = GjennomforingResponseDTO(
//                id = UUID.randomUUID(),
//                navn = "",
//                type = "",
//                arrangor = ArrangorResponseDTO(
//                    virksomhetsnummer = "",
//                    navn = "",
//                ),
//                valp = null,
//            ),
//            startDato = LocalDate.MIN,
//            sluttDato = LocalDate.MAX,
//            status = DeltakerStatusResponseDTO.DELTAR,
//            dagerPerUke = null,
//            prosentStilling = null,
//            registrertDato = LocalDateTime.MIN,
//        )
//    }
//
// //    private val kometResponse = KometResponse(
// //
// //    )
// //
// //    private val valpResponse = ValpResponse(
// //
// //    )
// }
