package no.fdk.fdk_reasoning_service.kafka

import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class KafkaHarvestedEventConsumer(
    private val circuitBreaker: KafkaHarvestedEventCircuitBreaker
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
        concurrency = "4",
        containerFactory = "kafkaListenerContainerFactory",
        id = REASONING_LISTENER_ID
    )
    fun listen(record: ConsumerRecord<String, SpecificRecord>, ack: Acknowledgment) {
        try {
            circuitBreaker.process(record)
            ack.acknowledge()
        } catch (e: Exception) {
            ack.nack(Duration.ZERO)
        }
    }

    companion object Const {
        const val REASONING_LISTENER_ID = "reasoning"
    }
}
