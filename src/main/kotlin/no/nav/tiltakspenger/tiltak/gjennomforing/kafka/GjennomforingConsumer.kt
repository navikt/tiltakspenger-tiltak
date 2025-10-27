package no.nav.tiltakspenger.tiltak.gjennomforing.kafka

import com.fasterxml.jackson.module.kotlin.readValue
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.kafka.Consumer
import no.nav.tiltakspenger.libs.kafka.ManagedKafkaConsumer
import no.nav.tiltakspenger.libs.kafka.config.KafkaConfig
import no.nav.tiltakspenger.libs.kafka.config.KafkaConfigImpl
import no.nav.tiltakspenger.libs.kafka.config.LocalKafkaConfig
import no.nav.tiltakspenger.tiltak.Configuration
import no.nav.tiltakspenger.tiltak.KAFKA_CONSUMER_GROUP_ID
import no.nav.tiltakspenger.tiltak.gjennomforing.db.GjennomforingRepo
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.UUIDDeserializer
import java.util.UUID

class GjennomforingConsumer(
    private val gjennomforingRepo: GjennomforingRepo,
    topic: String,
    groupId: String = KAFKA_CONSUMER_GROUP_ID,
    kafkaConfig: KafkaConfig = if (Configuration.isNais()) KafkaConfigImpl(autoOffsetReset = "earliest") else LocalKafkaConfig(),
) : Consumer<UUID, String?> {
    private val log = KotlinLogging.logger { }

    private val consumer = ManagedKafkaConsumer(
        topic = topic,
        config = kafkaConfig.consumerConfig(
            keyDeserializer = UUIDDeserializer(),
            valueDeserializer = StringDeserializer(),
            groupId = groupId,
        ),
        consume = ::consume,
    )

    override suspend fun consume(key: UUID, value: String?) {
        log.info { "Mottatt tiltaksgjennomføring med id $key" }
        if (value != null) {
            val tiltaksgjennomforingV1Dto = objectMapper.readValue<TiltaksgjennomforingV1Dto>(value)
            gjennomforingRepo.lagre(tiltaksgjennomforingV1Dto.toGjennomforing())
            log.info { "Lagret tiltaksgjennomføring med id $key" }
        } else {
            gjennomforingRepo.slett(key)
            log.warn { "Slettet tiltaksgjennomføring med id $key" }
        }
    }

    override fun run() = consumer.run()
}
