package no.fdk.reasoning.metrics

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Metrics
import no.fdk.reasoning.model.CatalogType

object ReasonedEventMetrics {
    private var registry: MeterRegistry = Metrics.globalRegistry

    fun bind(registry: MeterRegistry) {
        this.registry = registry
    }

    fun recordPublish(
        catalogType: CatalogType,
        kind: PublishKind,
        outcome: PublishOutcome,
    ) {
        registry
            .counter(
                "reasoned_event_publish_total",
                "status",
                outcome.status,
                "reason",
                outcome.reason,
                "type",
                ReasoningMetrics.metricType(catalogType),
                "kind",
                kind.label,
            ).increment()
    }

    enum class PublishKind(
        val label: String,
    ) {
        REASONED("reasoned"),
        HARVEST("harvest"),
    }

    enum class PublishOutcome(
        val status: String,
        val reason: String,
    ) {
        SUCCESS("success", "published"),
        PUBLISH_FAILED("error", "publish_failed"),
        SKIPPED("error", "skipped"),
    }
}
