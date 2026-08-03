package no.fdk.reasoning.metrics

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.fdk.reasoning.model.CatalogType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("unit")
class ReasonedEventMetricsTest {
    private lateinit var registry: SimpleMeterRegistry

    @BeforeEach
    fun setUp() {
        registry = SimpleMeterRegistry()
        ReasonedEventMetrics.bind(registry)
    }

    @AfterEach
    fun tearDown() {
        registry.clear()
    }

    @Test
    fun `recordPublish increments reasoned_event_publish_total`() {
        ReasonedEventMetrics.recordPublish(
            catalogType = CatalogType.DATASETS,
            kind = ReasonedEventMetrics.PublishKind.REASONED,
            outcome = ReasonedEventMetrics.PublishOutcome.SUCCESS,
        )
        ReasonedEventMetrics.recordPublish(
            catalogType = CatalogType.CONCEPTS,
            kind = ReasonedEventMetrics.PublishKind.HARVEST,
            outcome = ReasonedEventMetrics.PublishOutcome.PUBLISH_FAILED,
        )

        assertEquals(
            1.0,
            registry
                .counter(
                    "reasoned_event_publish_total",
                    "status",
                    "success",
                    "reason",
                    "published",
                    "type",
                    "datasets",
                    "kind",
                    "reasoned",
                ).count(),
        )
        assertEquals(
            1.0,
            registry
                .counter(
                    "reasoned_event_publish_total",
                    "status",
                    "error",
                    "reason",
                    "publish_failed",
                    "type",
                    "concepts",
                    "kind",
                    "harvest",
                ).count(),
        )
    }
}
