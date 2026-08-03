package no.fdk.reasoning.config

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.micrometer.tagged.TaggedCircuitBreakerMetrics
import io.micrometer.core.instrument.MeterRegistry
import jakarta.annotation.PostConstruct
import no.fdk.reasoning.metrics.KafkaReasoningMetrics
import no.fdk.reasoning.metrics.ReasonedEventMetrics
import no.fdk.reasoning.metrics.ReasoningMetrics
import no.fdk.reasoning.metrics.ReferenceDataMetrics
import org.springframework.context.annotation.Configuration

/** Binds Resilience4j circuit breaker metrics and custom reasoning metrics to the application [MeterRegistry]. */
@Configuration
open class MetricsConfiguration(
    private val circuitBreakerRegistry: CircuitBreakerRegistry,
    private val meterRegistry: MeterRegistry,
) {
    @PostConstruct
    fun bindMetrics() {
        ReasoningMetrics.bind(meterRegistry)
        KafkaReasoningMetrics.bind(meterRegistry)
        ReasonedEventMetrics.bind(meterRegistry)
        ReferenceDataMetrics.bind(meterRegistry)
        KafkaReasoningMetrics.registerListenerPausedGauge()

        TaggedCircuitBreakerMetrics
            .ofCircuitBreakerRegistry(circuitBreakerRegistry)
            .bindTo(meterRegistry)
    }
}
