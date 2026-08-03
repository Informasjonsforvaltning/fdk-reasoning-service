package no.fdk.reasoning.metrics

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.fdk.reasoning.model.CatalogType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.milliseconds

@Tag("unit")
class ReasoningMetricsTest {
    private lateinit var registry: SimpleMeterRegistry

    @BeforeEach
    fun setUp() {
        registry = SimpleMeterRegistry()
        ReasoningMetrics.bind(registry)
    }

    @AfterEach
    fun tearDown() {
        registry.clear()
    }

    @Test
    fun `recordTotal records reasoning timer`() {
        ReasoningMetrics.recordTotal(CatalogType.DATASETS, 100.milliseconds)

        assertEquals(
            1L,
            registry
                .timer("reasoning", "type", "datasets")
                .count(),
        )
    }

    @Test
    fun `recordStep records step timer`() {
        ReasoningMetrics.recordStep(ReasoningMetrics.Step.DEDUCTION, CatalogType.CONCEPTS, 50.milliseconds)

        assertEquals(
            1L,
            registry
                .timer("reasoning.deduction", "type", "concepts")
                .count(),
        )
    }

    @Test
    fun `recordError increments reasoning_error`() {
        ReasoningMetrics.recordError(CatalogType.DATASERVICES)

        assertEquals(
            1.0,
            registry
                .counter("reasoning_error", "type", "dataservices")
                .count(),
        )
    }
}
