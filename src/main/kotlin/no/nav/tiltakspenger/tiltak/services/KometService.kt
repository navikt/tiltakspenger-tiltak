package no.nav.tiltakspenger.tiltak.services

import mu.KotlinLogging
import mu.withLoggingContext
import net.logstash.logback.argument.StructuredArguments
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.rapids_rivers.asOptionalLocalDate
import java.time.LocalDate
import java.time.LocalDateTime

data class OppdaterTiltakDTO(
    val id: String,
    val gjennomforingId: String,
    val personIdent: String,
    val startDato: LocalDate?,
    val sluttDato: LocalDate?,
    val status: DeltakerStatusDto,
    val registrertDato: LocalDateTime,
    val dagerPerUke: Float?,
    val prosentStilling: Float?,
    val endretDato: LocalDateTime,
    val kilde: String?,
)

data class DeltakerStatusDto(
    val type: String,
    val aarsak: String?,
    val opprettetDato: LocalDateTime,
)

class KometService(
    rapidsConnection: RapidsConnection,
) : River.PacketListener {

    private val SECURELOG = KotlinLogging.logger("tjenestekall")
    private val LOG = KotlinLogging.logger {}

    init {
        River(rapidsConnection).apply {
            validate {
                it.demandKey("id")
                it.demandKey("gjennomforingId")
                it.forbid("@løsning")
                it.forbid("@behovId")

                it.requireKey("personIdent")
                it.interestedIn("startDato")
                it.interestedIn("sluttDato")
                it.requireKey("status")
                it.requireKey("status.type")
                it.interestedIn("status.aarsak")
                it.requireKey("status.opprettetDato")
                it.requireKey("registrertDato")
                it.interestedIn("dagerPerUke")
                it.interestedIn("prosentStilling")
                it.requireKey("endretDato")
                it.interestedIn("kilde")
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        runCatching {
            loggVedInngang(packet)

            withLoggingContext(
                "id" to packet["id"].asText(),
            ) {
                // val ident = packet["personIdent"].asText()

                SECURELOG.info { "mottok melding: ${packet.toJson()}" }
                val oppdatertTiltak = OppdaterTiltakDTO(
                    id = packet["id"].asText(),
                    gjennomforingId = packet["gjennomforingId"].asText(),
                    personIdent = packet["personIdent"].asText(),
                    startDato = packet["startDato"].asOptionalLocalDate(),
                    sluttDato = packet["sluttDato"].asOptionalLocalDate(),
                    status = DeltakerStatusDto(
                        type = packet["status.type"].asText(),
                        aarsak = packet["status.aarsak"].asText(),
                        opprettetDato = packet["status.opprettetDato"].asLocalDateTime(),
                    ),
                    registrertDato = packet["registrertDato"].asLocalDateTime(),
                    dagerPerUke = packet["dagerPerUke"].asDouble().toFloat(),
                    prosentStilling = packet["prosentStilling"].asDouble().toFloat(),
                    endretDato = packet["endretDato"].asLocalDateTime(),
                    kilde = packet["kilde"].asText(),
                )

                packet["@løsning"] = mapOf(
                    "oppdatertTiltak" to oppdatertTiltak,
                )
                loggVedUtgang(packet)
                SECURELOG.info { "publiserer melding: ${packet.toJson()}" }
                // context.publish(ident, packet.toJson())
            }
        }.onFailure {
            loggVedFeil(it, packet)
        }.getOrThrow()
    }

    override fun onError(problems: MessageProblems, context: MessageContext) {
        if (context.rapidName() == "amt-deltaker-v1") {
            LOG.error { "onError" }
            LOG.error { problems.toExtendedReport() }
        }
    }

    override fun onSevere(error: MessageProblems.MessageException, context: MessageContext) {
        if (context.rapidName() == "amt-deltaker-v1") {
            LOG.error { "onSevere" }
            LOG.error { error.problems.toExtendedReport() }
        }
    }

    private fun loggVedInngang(packet: JsonMessage) {
        LOG.info(
            "fikk endring av tiltak fra komet {} og {}",
            StructuredArguments.keyValue("id", packet["@id"].asText()),
            StructuredArguments.keyValue("behovId", packet["@behovId"].asText()),
        )
        SECURELOG.info(
            "fikk endring av tiltak fra komet {} og {}",
            StructuredArguments.keyValue("id", packet["@id"].asText()),
            StructuredArguments.keyValue("behovId", packet["@behovId"].asText()),
        )
        SECURELOG.debug { "mottok melding: ${packet.toJson()}" }
    }

    private fun loggVedUtgang(packet: JsonMessage) {
        LOG.info(
            "sender melding om endring av tiltak {} og {}",
            StructuredArguments.keyValue("id", packet["@id"].asText()),
            StructuredArguments.keyValue("behovId", packet["@behovId"].asText()),
        )
        SECURELOG.info(
            "sender melding om endring av tiltak {} og {}",
            StructuredArguments.keyValue("id", packet["@id"].asText()),
            StructuredArguments.keyValue("behovId", packet["@behovId"].asText()),
        )
        SECURELOG.debug { "publiserer melding: ${packet.toJson()}" }
    }

    private fun loggVedFeil(ex: Throwable, packet: JsonMessage) {
        LOG.error(
            "feil ved behandling av tiltak-behov med {}, se securelogs for detaljer",
            StructuredArguments.keyValue("id", packet["@id"].asText()),
        )
        SECURELOG.error(
            "feil \"${ex.message}\" ved behandling av tiltak-behov med {} og {}",
            StructuredArguments.keyValue("id", packet["@id"].asText()),
            StructuredArguments.keyValue("packet", packet.toJson()),
            ex,
        )
    }
}
