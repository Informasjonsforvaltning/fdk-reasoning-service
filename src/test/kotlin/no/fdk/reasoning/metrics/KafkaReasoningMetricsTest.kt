package no.fdk.reasoning.metrics

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.fdk.reasoning.model.CatalogType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("unit")
class KafkaReasoningMetricsTest {
    private lateinit var registry: SimpleMeterRegistry

    @BeforeEach
    fun setUp() {
        registry = SimpleMeterRegistry()
        KafkaReasoningMetrics.bind(registry)
    }

    @AfterEach
    fun tearDown() {
        KafkaReasoningMetrics.setListenerPaused(false)
        registry.clear()
    }

    @Test
    fun `recordEventProcessed increments reasoning_event_processing_total`() {
        KafkaReasoningMetrics.recordEventProcessed(
            CatalogType.DATASETS,
            KafkaReasoningMetrics.EventProcessingResult.ACKED,
        )
        KafkaReasoningMetrics.recordEventProcessed(
            null,
            KafkaReasoningMetrics.EventProcessingResult.SKIPPED,
        )
        KafkaReasoningMetrics.recordEventProcessed(
            CatalogType.CONCEPTS,
            KafkaReasoningMetrics.EventProcessingResult.NACKED,
        )
        KafkaReasoningMetrics.recordEventProcessed(
            null,
            KafkaReasoningMetrics.EventProcessingResult.CIRCUIT_OPEN,
        )

        assertEquals(
            1.0,
            registry
                .counter(
                    "reasoning_event_processing_total",
                    "type",
                    "datasets",
                    "result",
                    "acked",
                ).count(),
        )
        assertEquals(
            1.0,
            registry
                .counter(
                    "reasoning_event_processing_total",
                    "type",
                    "unknown",
                    "result",
                    "skipped",
                ).count(),
        )
        assertEquals(
            1.0,
            registry
                .counter(
                    "reasoning_event_processing_total",
                    "type",
                    "concepts",
                    "result",
                    "nacked",
                ).count(),
        )
        assertEquals(
            1.0,
            registry
                .counter(
                    "reasoning_event_processing_total",
                    "type",
                    "unknown",
                    "result",
                    "circuit_open",
                ).count(),
        )
    }

    @Test
    fun `registerListenerPausedGauge exposes gauge before first state change`() {
        KafkaReasoningMetrics.registerListenerPausedGauge()

        assertEquals(0.0, registry.find("kafka_listener_paused").gauge()?.value())
    }

    @Test
    fun `setListenerPaused updates kafka_listener_paused gauge`() {
        KafkaReasoningMetrics.registerListenerPausedGauge()
        KafkaReasoningMetrics.setListenerPaused(true)
        assertEquals(1.0, registry.find("kafka_listener_paused").gauge()?.value())

        KafkaReasoningMetrics.setListenerPaused(false)
        assertEquals(0.0, registry.find("kafka_listener_paused").gauge()?.value())
    }
}
