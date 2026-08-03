package no.fdk.reasoning.metrics

import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Metrics
import no.fdk.reasoning.model.CatalogType
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

object KafkaReasoningMetrics {
    private var registry: MeterRegistry = Metrics.globalRegistry
    private val listenerPaused = AtomicInteger(0)
    private val gaugeRegistered = AtomicBoolean(false)

    fun bind(registry: MeterRegistry) {
        if (this.registry !== registry) {
            this.registry = registry
            gaugeRegistered.set(false)
        }
    }

    fun registerListenerPausedGauge() {
        ensureListenerPausedGaugeRegistered()
    }

    fun recordEventProcessed(
        catalogType: CatalogType?,
        result: EventProcessingResult,
    ) {
        registry
            .counter(
                "reasoning_event_processing_total",
                "type",
                catalogType?.let { ReasoningMetrics.metricType(it) } ?: "unknown",
                "result",
                result.label,
            ).increment()
    }

    fun setListenerPaused(paused: Boolean) {
        ensureListenerPausedGaugeRegistered()
        listenerPaused.set(if (paused) 1 else 0)
    }

    private fun ensureListenerPausedGaugeRegistered() {
        if (gaugeRegistered.compareAndSet(false, true)) {
            Gauge
                .builder("kafka_listener_paused") { listenerPaused.get().toDouble() }
                .description("1 when the reasoning Kafka listener is paused, otherwise 0")
                .tag("listener", "reasoning")
                .register(registry)
        }
    }

    enum class EventProcessingResult(
        val label: String,
    ) {
        ACKED("acked"),
        NACKED("nacked"),
        SKIPPED("skipped"),
        CIRCUIT_OPEN("circuit_open"),
    }
}
