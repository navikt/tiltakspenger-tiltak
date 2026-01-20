package no.nav.tiltakspenger.tiltak.clients.tiltakshistorikk.dto

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.time.LocalDate
import java.util.UUID

/**
 * Hentet fra https://github.com/navikt/mulighetsrommet/blob/main/common/tiltakshistorikk-client/src/main/kotlin/no/nav/tiltak/historikk/TiltakshistorikkV1Dto.kt
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = TiltakshistorikkV1Dto.ArenaDeltakelse::class, name = "ArenaDeltakelse"),
    JsonSubTypes.Type(value = TiltakshistorikkV1Dto.TeamKometDeltakelse::class, name = "TeamKometDeltakelse"),
    JsonSubTypes.Type(value = TiltakshistorikkV1Dto.TeamTiltakAvtale::class, name = "TeamTiltakAvtale"),
)
sealed interface TiltakshistorikkV1Dto {
    /**
     * Id på deltakelse fra kildesystemet.
     *
     * MERK: Hvis kildesystemet er Arena så vil dette være en id som kun er kjent i `tiltakshistorikk`,
     * id fra Arena er tilgjengelig i feltet [TiltakshistorikkV1Dto.ArenaDeltakelse.arenaId].
     */
    val id: UUID

    /**
     * Hvilket kildesystem deltakelsen kommer fra.
     */
    val opphav: Opphav

    /**
     * Startdato i tiltaket.
     */
    val startDato: LocalDate?

    /**
     * Sluttdato i tiltaket.
     */
    val sluttDato: LocalDate?

    /**
     * Beskrivende tittel/leslig navn for tiltaksdeltakelsen.
     *
     * Dette vises bl.a. til veileder i Modia og til bruker i aktivitetsplanen (for noen tiltak), og vil typisk være på
     * formatet "<tiltakstype> hos <arrangør>", f.eks. "Oppfølging hos Arrangør AS".
     *
     * Selve innholdet/oppbygning av tittelen kan variere mellom de forskjellige tiltakstypene og det kan komme
     * endringer i logikken på hvordan dette utledes.
     */
    val tittel: String

    enum class Opphav {
        ARENA,
        TEAM_KOMET,
        TEAM_TILTAK,
    }

    data class Virksomhet(
        /**
         * Navn på virksomhet vil stort sett være tilgjenglig, men i noen sjeldne tilfeller så kan det være
         * at dette er ukjent (typisk for eldre tiltaksdeltakelser).
         */
        val navn: String?,
    )

    data class Arrangor(
        /**
         * Hovedenhet/Juridisk enhet hos arrangør (fra brreg)
         */
        val hovedenhet: Virksomhet?,

        /**
         * Underenhet hos arrangør (fra brreg) som tiltaksgjennomføringen er registrert på.
         */
        val underenhet: Virksomhet,
    )

    data class Gjennomforing(
        val id: UUID,
        /**
         * Deltidsprosent kan være definert på gjennomføringen og vil i så fall gjelde for alle deltakelser
         * på tiltaket (med mindre en annen deltidsprosent er definert på deltakelsen).
         */
        val deltidsprosent: Float?,
    )

    data class ArenaDeltakelse(
        override val startDato: LocalDate?,
        override val sluttDato: LocalDate?,
        override val id: UUID,
        override val tittel: String,
        val arenaId: Int,
        val status: ArenaDeltakerStatusDto,
        val tiltakstype: Tiltakstype,
        val gjennomforing: Gjennomforing,
        val arrangor: Arrangor,
        val deltidsprosent: Float?,
        val dagerPerUke: Float?,
    ) : TiltakshistorikkV1Dto {
        override val opphav = Opphav.ARENA

        data class Tiltakstype(
            val tiltakskode: String,
            val navn: String,
        )
    }

    data class TeamKometDeltakelse(
        override val startDato: LocalDate?,
        override val sluttDato: LocalDate?,
        override val id: UUID,
        override val tittel: String,
        val status: KometDeltakerStatusDto,
        val tiltakstype: Tiltakstype,
        val gjennomforing: Gjennomforing,
        val arrangor: Arrangor,
        val deltidsprosent: Float?,
        val dagerPerUke: Float?,
    ) : TiltakshistorikkV1Dto {
        override val opphav = Opphav.TEAM_KOMET

        data class Tiltakstype(
            val tiltakskode: TiltakskodeDto,
            val navn: String,
        )
    }

    data class TeamTiltakAvtale(
        override val startDato: LocalDate?,
        override val sluttDato: LocalDate?,
        override val id: UUID,
        override val tittel: String,
        val tiltakstype: Tiltakstype,
        val status: ArbeidsgiverAvtaleStatusDto,
        val arbeidsgiver: Virksomhet,
    ) : TiltakshistorikkV1Dto {
        override val opphav = Opphav.TEAM_TILTAK

        data class Tiltakstype(
            val tiltakskode: Tiltakskode,
            val navn: String,
        )

        enum class Tiltakskode {
            ARBEIDSTRENING,
            MIDLERTIDIG_LONNSTILSKUDD,
            VARIG_LONNSTILSKUDD,
            MENTOR,
            INKLUDERINGSTILSKUDD,
            SOMMERJOBB,
            VTAO,
        }
    }
}

data class TiltakshistorikkV1Response(
    val historikk: List<TiltakshistorikkV1Dto>,
)
