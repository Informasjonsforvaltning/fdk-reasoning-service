package no.fdk.reasoning.kafka

import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import no.fdk.reasoning.metrics.KafkaReasoningMetrics
import no.fdk.reasoning.metrics.KafkaReasoningMetrics.EventProcessingResult
import no.fdk.reasoning.model.CatalogType
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class KafkaHarvestedEventConsumer(private val circuitBreaker: KafkaHarvestedEventCircuitBreaker) {
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
    fun listen(record: ConsumerRecord<String, Any?>, ack: Acknowledgment) {
        LOGGER.debug("Listener received record - topic: {} partition: {} offset: {}", record.topic(), record.partition(), record.offset())
        try {
            if (record.value() == null) {
                LOGGER.debug(
                    "Ignoring null value - topic: {} partition: {} offset: {}",
                    record.topic(),
                    record.partition(),
                    record.offset(),
                )
                KafkaReasoningMetrics.recordEventProcessed(null, EventProcessingResult.SKIPPED)
                ack.acknowledge()
                return
            }
            when (val outcome = circuitBreaker.process(record)) {
                is KafkaHarvestedEventCircuitBreaker.ProcessOutcome.Skipped -> {
                    KafkaReasoningMetrics.recordEventProcessed(null, EventProcessingResult.SKIPPED)
                    ack.acknowledge()
                }

                is KafkaHarvestedEventCircuitBreaker.ProcessOutcome.Success -> {
                    KafkaReasoningMetrics.recordEventProcessed(outcome.catalogType, EventProcessingResult.ACKED)
                    ack.acknowledge()
                }
            }
        } catch (e: CallNotPermittedException) {
            LOGGER.warn(
                "Circuit breaker open, nacking message - topic: {} partition: {} offset: {}",
                record.topic(),
                record.partition(),
                record.offset(),
            )
            KafkaReasoningMetrics.recordEventProcessed(null, EventProcessingResult.CIRCUIT_OPEN)
            ack.nack(Duration.ZERO)
        } catch (e: ReasoningProcessingException) {
            LOGGER.warn(
                "Reasoning failed, nacking message - topic: {} partition: {} offset: {} error: {}",
                record.topic(),
                record.partition(),
                record.offset(),
                e.message,
                e,
            )
            KafkaReasoningMetrics.recordEventProcessed(e.catalogType, EventProcessingResult.NACKED)
            ack.nack(Duration.ZERO)
        } catch (e: Exception) {
            LOGGER.warn(
                "Reasoning failed, nacking message - topic: {} partition: {} offset: {} error: {}",
                record.topic(),
                record.partition(),
                record.offset(),
                e.message,
                e,
            )
            KafkaReasoningMetrics.recordEventProcessed(null, EventProcessingResult.NACKED)
            ack.nack(Duration.ZERO)
        }
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(KafkaHarvestedEventConsumer::class.java)
        const val REASONING_LISTENER_ID = "reasoning"
    }
}

class ReasoningProcessingException(val catalogType: CatalogType, cause: Throwable) : RuntimeException(cause.message, cause)
