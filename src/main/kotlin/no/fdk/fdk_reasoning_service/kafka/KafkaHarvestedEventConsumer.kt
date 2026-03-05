package no.fdk.fdk_reasoning_service.kafka

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class KafkaHarvestedEventConsumer(
    private val circuitBreaker: KafkaHarvestedEventCircuitBreaker,
) {
    @KafkaListener(
        topics = [
            "dataset-events",
            "concept-events",
            "data-service-events",
            "information-model-events",
            "service-events",
            "event-events",
        ],
        groupId = "fdk-reasoning-service",
        containerFactory = "kafkaListenerContainerFactory",
        id = REASONING_LISTENER_ID,
    )
    fun listen(
        record: ConsumerRecord<String, Any?>,
        ack: Acknowledgment,
    ) {
        LOGGER.debug("Listener received record - topic: {} partition: {} offset: {}", record.topic(), record.partition(), record.offset())
        try {
            if (record.value() == null) {
                LOGGER.debug("Ignoring null value - topic: {} partition: {} offset: {}", record.topic(), record.partition(), record.offset())
                ack.acknowledge()
                return
            }
            circuitBreaker.process(record)
            ack.acknowledge()
        } catch (e: Exception) {
            LOGGER.warn(
                "Reasoning failed, nacking message - topic: {} partition: {} offset: {} error: {}",
                record.topic(),
                record.partition(),
                record.offset(),
                e.message,
                e,
            )
            ack.nack(Duration.ZERO)
        }
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(KafkaHarvestedEventConsumer::class.java)
        const val REASONING_LISTENER_ID = "reasoning"
    }
}
