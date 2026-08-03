package no.fdk.reasoning.metrics

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Metrics
import kotlin.time.Duration
import kotlin.time.toJavaDuration

object ReferenceDataMetrics {
    private var registry: MeterRegistry = Metrics.globalRegistry

    fun bind(registry: MeterRegistry) {
        this.registry = registry
    }

    fun recordRefresh(
        source: String,
        success: Boolean,
        duration: Duration,
    ) {
        val sourceLabel = metricSource(source)
        registry
            .counter(
                "reference_data_refresh_total",
                "status",
                if (success) "success" else "error",
                "source",
                sourceLabel,
            ).increment()
        registry
            .timer(
                "reference_data_refresh_time",
                "source",
                sourceLabel,
                "status",
                if (success) "success" else "error",
            ).record(duration.toJavaDuration())
    }

    fun metricSource(label: String): String =
        label
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
}
