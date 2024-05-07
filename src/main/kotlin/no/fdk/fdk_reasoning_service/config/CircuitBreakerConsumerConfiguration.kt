package no.fdk.fdk_reasoning_service.config

import no.fdk.fdk_reasoning_service.kafka.KafkaManager
import io.github.resilience4j.circuitbreaker.CircuitBreaker.StateTransition
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnStateTransitionEvent
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration

@Configuration
open class CircuitBreakerConsumerConfiguration(
    private val circuitBreakerRegistry: CircuitBreakerRegistry,
    private val kafkaManager: KafkaManager
) {

    init {
        LOGGER.debug("Configuring circuit breaker event listener")
        circuitBreakerRegistry.circuitBreaker("dataset-reasoning").eventPublisher.onStateTransition { event: CircuitBreakerOnStateTransitionEvent ->
            when (event.stateTransition) {
                StateTransition.CLOSED_TO_OPEN,
                StateTransition.CLOSED_TO_FORCED_OPEN,
                StateTransition.HALF_OPEN_TO_OPEN -> kafkaManager.pause("dataset-reasoning")

                StateTransition.OPEN_TO_HALF_OPEN,
                StateTransition.HALF_OPEN_TO_CLOSED,
                StateTransition.FORCED_OPEN_TO_CLOSED,
                StateTransition.FORCED_OPEN_TO_HALF_OPEN -> kafkaManager.resume("dataset-reasoning")

                else -> throw IllegalStateException("Unknown transition state: " + event.stateTransition)
            }
        }

        circuitBreakerRegistry.circuitBreaker("concept-reasoning").eventPublisher.onStateTransition { event: CircuitBreakerOnStateTransitionEvent ->
            when (event.stateTransition) {
                StateTransition.CLOSED_TO_OPEN,
                StateTransition.CLOSED_TO_FORCED_OPEN,
                StateTransition.HALF_OPEN_TO_OPEN -> kafkaManager.pause("concept-reasoning")

                StateTransition.OPEN_TO_HALF_OPEN,
                StateTransition.HALF_OPEN_TO_CLOSED,
                StateTransition.FORCED_OPEN_TO_CLOSED,
                StateTransition.FORCED_OPEN_TO_HALF_OPEN -> kafkaManager.resume("concept-reasoning")

                else -> throw IllegalStateException("Unknown transition state: " + event.stateTransition)
            }
        }

        circuitBreakerRegistry.circuitBreaker("data-service-reasoning").eventPublisher.onStateTransition { event: CircuitBreakerOnStateTransitionEvent ->
            when (event.stateTransition) {
                StateTransition.CLOSED_TO_OPEN,
                StateTransition.CLOSED_TO_FORCED_OPEN,
                StateTransition.HALF_OPEN_TO_OPEN -> kafkaManager.pause("data-service-reasoning")

                StateTransition.OPEN_TO_HALF_OPEN,
                StateTransition.HALF_OPEN_TO_CLOSED,
                StateTransition.FORCED_OPEN_TO_CLOSED,
                StateTransition.FORCED_OPEN_TO_HALF_OPEN -> kafkaManager.resume("data-service-reasoning")

                else -> throw IllegalStateException("Unknown transition state: " + event.stateTransition)
            }
        }

        circuitBreakerRegistry.circuitBreaker("information-model-reasoning").eventPublisher.onStateTransition { event: CircuitBreakerOnStateTransitionEvent ->
            when (event.stateTransition) {
                StateTransition.CLOSED_TO_OPEN,
                StateTransition.CLOSED_TO_FORCED_OPEN,
                StateTransition.HALF_OPEN_TO_OPEN -> kafkaManager.pause("information-model-reasoning")

                StateTransition.OPEN_TO_HALF_OPEN,
                StateTransition.HALF_OPEN_TO_CLOSED,
                StateTransition.FORCED_OPEN_TO_CLOSED,
                StateTransition.FORCED_OPEN_TO_HALF_OPEN -> kafkaManager.resume("information-model-reasoning")

                else -> throw IllegalStateException("Unknown transition state: " + event.stateTransition)
            }
        }

        circuitBreakerRegistry.circuitBreaker("service-reasoning").eventPublisher.onStateTransition { event: CircuitBreakerOnStateTransitionEvent ->
            when (event.stateTransition) {
                StateTransition.CLOSED_TO_OPEN,
                StateTransition.CLOSED_TO_FORCED_OPEN,
                StateTransition.HALF_OPEN_TO_OPEN -> kafkaManager.pause("service-reasoning")

                StateTransition.OPEN_TO_HALF_OPEN,
                StateTransition.HALF_OPEN_TO_CLOSED,
                StateTransition.FORCED_OPEN_TO_CLOSED,
                StateTransition.FORCED_OPEN_TO_HALF_OPEN -> kafkaManager.resume("service-reasoning")

                else -> throw IllegalStateException("Unknown transition state: " + event.stateTransition)
            }
        }

        circuitBreakerRegistry.circuitBreaker("event-reasoning").eventPublisher.onStateTransition { event: CircuitBreakerOnStateTransitionEvent ->
            when (event.stateTransition) {
                StateTransition.CLOSED_TO_OPEN,
                StateTransition.CLOSED_TO_FORCED_OPEN,
                StateTransition.HALF_OPEN_TO_OPEN -> kafkaManager.pause("event-reasoning")

                StateTransition.OPEN_TO_HALF_OPEN,
                StateTransition.HALF_OPEN_TO_CLOSED,
                StateTransition.FORCED_OPEN_TO_CLOSED,
                StateTransition.FORCED_OPEN_TO_HALF_OPEN -> kafkaManager.resume("event-reasoning")

                else -> throw IllegalStateException("Unknown transition state: " + event.stateTransition)
            }
        }
    }

    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(CircuitBreakerConsumerConfiguration::class.java)
    }
}
