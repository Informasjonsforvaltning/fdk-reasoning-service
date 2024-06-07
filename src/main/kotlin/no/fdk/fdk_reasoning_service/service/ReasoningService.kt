package no.fdk.fdk_reasoning_service.service

import io.micrometer.core.instrument.Metrics
import no.fdk.fdk_reasoning_service.model.CatalogType
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
    fun reasonGraph(
        graph: String,
        catalogType: CatalogType,
    ): String {
        val inputModel = parseRDFResponse(graph, Lang.TURTLE)

        val deductionReasoning =
            measureTimedValue {
                deductionService.reason(inputModel, catalogType)
            }
        val organizationReasoning =
            measureTimedValue {
                organizationService.reason(inputModel, catalogType)
            }
        val referenceDataReasoning =
            measureTimedValue {
                referenceDataService.reason(inputModel, catalogType)
            }
        val themeReasoning =
            measureTimedValue {
                themeService.reason(inputModel, catalogType)
            }

        Metrics.timer(
            "reasoning.deduction",
            "type",
            catalogType.toString().lowercase(),
        ).record(deductionReasoning.duration.toJavaDuration())

        Metrics.timer(
            "reasoning.organization",
            "type",
            catalogType.toString().lowercase(),
        ).record(organizationReasoning.duration.toJavaDuration())

        Metrics.timer(
            "reasoning.reference_data",
            "type",
            catalogType.toString().lowercase(),
        ).record(referenceDataReasoning.duration.toJavaDuration())

        Metrics.timer(
            "reasoning.themes",
            "type",
            catalogType.toString().lowercase(),
        ).record(themeReasoning.duration.toJavaDuration())

        return ModelFactory.createDefaultModel()
            .add(deductionReasoning.value)
            .add(organizationReasoning.value)
            .add(referenceDataReasoning.value)
            .add(themeReasoning.value)
            .add(inputModel)
            .createRDFResponse(Lang.TURTLE)
    }
}
