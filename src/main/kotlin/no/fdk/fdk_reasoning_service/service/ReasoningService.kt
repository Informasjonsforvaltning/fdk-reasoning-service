package no.fdk.fdk_reasoning_service.service

import io.micrometer.core.instrument.Metrics
import no.fdk.fdk_reasoning_service.model.CatalogType
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.riot.Lang
import org.springframework.stereotype.Service
import kotlin.time.measureTimedValue
import kotlin.time.toJavaDuration

@Service
class ReasoningService(
    private val organizationService: OrganizationService,
    private val referenceDataService: ReferenceDataService,
    private val deductionService: DeductionService,
    private val themeService: ThemeService,
) {
    private data class ReasoningStep(
        val metric: String,
        val useCatalogGraph: Boolean,
        val reason: (Model, CatalogType) -> Model,
    )

    fun reasonGraph(
        graph: String,
        catalogType: CatalogType,
        catalogGraph: String?,
    ): String {
        val inputModel = parseRDFResponse(graph, Lang.TURTLE)
        val inputModelWithCatalog = if (catalogGraph != null) {
            inputModel.union(parseRDFResponse(catalogGraph, Lang.TURTLE))
        } else {
            inputModel
        }

        val steps = listOf(
            ReasoningStep("reasoning.deduction", useCatalogGraph = true, deductionService::reason),
            ReasoningStep("reasoning.organization", useCatalogGraph = true, organizationService::reason),
            ReasoningStep("reasoning.reference_data", useCatalogGraph = false, referenceDataService::reason),
            ReasoningStep("reasoning.themes", useCatalogGraph = false, themeService::reason),
        )

        val reasonedModels = steps.map { step ->
            val input = if (step.useCatalogGraph) inputModelWithCatalog else inputModel
            val timed = measureTimedValue { step.reason(input, catalogType) }
            Metrics.timer(
                step.metric,
                "type",
                catalogType.toString().lowercase(),
            ).record(timed.duration.toJavaDuration())
            timed.value
        }

        return reasonedModels
            .fold(ModelFactory.createDefaultModel()) { acc, model -> acc.add(model) }
            .add(inputModel)
            .createRDFResponse(Lang.TURTLE)
    }
}
