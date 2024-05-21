package no.fdk.fdk_reasoning_service.config

import io.github.resilience4j.circuitbreaker.CircuitBreaker.StateTransition
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnStateTransitionEvent
import no.fdk.fdk_reasoning_service.kafka.KafkaHarvestedEventCircuitBreaker
import no.fdk.fdk_reasoning_service.kafka.KafkaHarvestedEventConsumer
import no.fdk.fdk_reasoning_service.kafka.KafkaManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration

@Configuration
open class CircuitBreakerConsumerConfiguration(
    private val circuitBreakerRegistry: CircuitBreakerRegistry,
    private val kafkaManager: KafkaManager,
) {
    init {
        LOGGER.debug("Configuring circuit breaker event listener")
        circuitBreakerRegistry.circuitBreaker(KafkaHarvestedEventCircuitBreaker.CIRCUIT_BREAKER_ID)
            .eventPublisher
            .onStateTransition { event: CircuitBreakerOnStateTransitionEvent ->
                handleStateTransition(event)
            }
    }

    private fun handleStateTransition(event: CircuitBreakerOnStateTransitionEvent) {
        when (event.stateTransition) {
            StateTransition.CLOSED_TO_OPEN,
            StateTransition.CLOSED_TO_FORCED_OPEN,
            StateTransition.HALF_OPEN_TO_OPEN,
            -> kafkaManager.pause(KafkaHarvestedEventConsumer.REASONING_LISTENER_ID)

            StateTransition.OPEN_TO_HALF_OPEN,
            StateTransition.HALF_OPEN_TO_CLOSED,
            StateTransition.FORCED_OPEN_TO_CLOSED,
            StateTransition.FORCED_OPEN_TO_HALF_OPEN,
            -> kafkaManager.resume(KafkaHarvestedEventConsumer.REASONING_LISTENER_ID)

            else -> throw IllegalStateException("Unknown transition state: " + event.stateTransition)
        }
    }

    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(CircuitBreakerConsumerConfiguration::class.java)
    }
}
