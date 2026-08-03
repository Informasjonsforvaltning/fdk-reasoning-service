package no.fdk.reasoning.metrics

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Metrics
import no.fdk.reasoning.model.CatalogType
import kotlin.time.Duration
import kotlin.time.toJavaDuration

object ReasoningMetrics {
    private var registry: MeterRegistry = Metrics.globalRegistry

    fun bind(registry: MeterRegistry) {
        this.registry = registry
    }

    fun recordTotal(
        catalogType: CatalogType,
        duration: Duration,
    ) {
        registry
            .timer(
                "reasoning",
                "type",
                metricType(catalogType),
            ).record(duration.toJavaDuration())
    }

    fun recordStep(
        step: Step,
        catalogType: CatalogType,
        duration: Duration,
    ) {
        registry
            .timer(
                step.metric,
                "type",
                metricType(catalogType),
            ).record(duration.toJavaDuration())
    }

    fun recordError(catalogType: CatalogType) {
        registry
            .counter(
                "reasoning_error",
                "type",
                metricType(catalogType),
            ).increment()
    }

    fun metricType(catalogType: CatalogType): String = catalogType.name.lowercase()

    enum class Step(
        val metric: String,
    ) {
        DEDUCTION("reasoning.deduction"),
        ORGANIZATION("reasoning.organization"),
        REFERENCE_DATA("reasoning.reference_data"),
        THEMES("reasoning.themes"),
    }
}
