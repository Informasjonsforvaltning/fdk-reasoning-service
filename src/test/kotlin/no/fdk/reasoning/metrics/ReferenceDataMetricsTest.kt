package no.fdk.reasoning.metrics

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.milliseconds

@Tag("unit")
class ReferenceDataMetricsTest {
    private lateinit var registry: SimpleMeterRegistry

    @BeforeEach
    fun setUp() {
        registry = SimpleMeterRegistry()
        ReferenceDataMetrics.bind(registry)
    }

    @AfterEach
    fun tearDown() {
        registry.clear()
    }

    @Test
    fun `metricSource normalizes labels`() {
        assertEquals("organization", ReferenceDataMetrics.metricSource("organization"))
        assertEquals("data_themes", ReferenceDataMetrics.metricSource("data themes"))
        assertEquals("iana_media_types", ReferenceDataMetrics.metricSource("IANA media types"))
        assertEquals("los", ReferenceDataMetrics.metricSource("LOS"))
    }

    @Test
    fun `recordRefresh increments counter and records timer`() {
        ReferenceDataMetrics.recordRefresh("data themes", success = true, duration = 25.milliseconds)
        ReferenceDataMetrics.recordRefresh("LOS", success = false, duration = 10.milliseconds)

        assertEquals(
            1.0,
            registry
                .counter(
                    "reference_data_refresh_total",
                    "status",
                    "success",
                    "source",
                    "data_themes",
                ).count(),
        )
        assertEquals(
            1.0,
            registry
                .counter(
                    "reference_data_refresh_total",
                    "status",
                    "error",
                    "source",
                    "los",
                ).count(),
        )
        assertEquals(
            1L,
            registry
                .timer(
                    "reference_data_refresh_time",
                    "source",
                    "data_themes",
                    "status",
                    "success",
                ).count(),
        )
    }
}
