package no.fdk.reasoning.service

import no.fdk.reasoning.metrics.ReasoningMetrics
import no.fdk.reasoning.metrics.ReasoningMetrics.Step
import no.fdk.reasoning.model.CatalogType
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.riot.Lang
import org.springframework.stereotype.Service
import kotlin.time.measureTimedValue

@Service
class ReasoningService(
    private val organizationService: OrganizationService,
    private val referenceDataService: ReferenceDataService,
    private val deductionService: DeductionService,
    private val themeService: ThemeService,
) {
    private data class ReasoningStep(
        val step: Step,
        val useCatalogGraph: Boolean,
        val reason: (Model, CatalogType) -> Model,
    )

    fun reasonGraph(
        graph: String,
        catalogType: CatalogType,
        catalogGraph: String?,
    ): String {
        val inputModel = parseRDFResponse(graph, Lang.TURTLE)
        val inputModelWithCatalog =
            if (catalogGraph != null) {
                inputModel.union(parseRDFResponse(catalogGraph, Lang.TURTLE))
            } else {
                inputModel
            }

        val steps =
            listOf(
                ReasoningStep(Step.DEDUCTION, useCatalogGraph = true, deductionService::reason),
                ReasoningStep(Step.ORGANIZATION, useCatalogGraph = true, organizationService::reason),
                ReasoningStep(Step.REFERENCE_DATA, useCatalogGraph = false, referenceDataService::reason),
                ReasoningStep(Step.THEMES, useCatalogGraph = false, themeService::reason),
            )

        val reasonedModels =
            steps.map { step ->
                val input = if (step.useCatalogGraph) inputModelWithCatalog else inputModel
                val timed = measureTimedValue { step.reason(input, catalogType) }
                ReasoningMetrics.recordStep(step.step, catalogType, timed.duration)
                timed.value
            }

        return reasonedModels
            .fold(ModelFactory.createDefaultModel()) { acc, model -> acc.add(model) }
            .add(inputModel)
            .createRDFResponse(Lang.TURTLE)
    }
}
