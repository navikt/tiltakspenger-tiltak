package no.nav.tiltakspenger.tiltak.gjennomforing.tiltakstype.kafka

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
import no.nav.tiltakspenger.tiltak.gjennomforing.tiltakstype.db.TiltakstypeRepo
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.UUIDDeserializer
import java.util.UUID

class TiltakstypeConsumer(
    private val tiltakstypeRepo: TiltakstypeRepo,
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
        log.info { "Mottatt tiltakstype med key $key" }
        if (value != null) {
            val tiltakstypeDto = objectMapper.readValue<TiltakstypeDto>(value)
            tiltakstypeRepo.lagre(tiltakstypeDto.toTiltakstype())
        } else {
            log.warn { "Mottatt tiltakstype med key $key uten value" }
        }
    }

    override fun run() = consumer.run()
}
