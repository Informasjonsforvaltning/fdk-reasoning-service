package no.fdk.fdk_reasoning_service.service

import no.fdk.fdk_reasoning_service.model.CatalogType
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.reasoner.rulesys.GenericRuleReasoner
import org.apache.jena.reasoner.rulesys.Rule
import org.springframework.stereotype.Service

@Service
class DeductionService() : Reasoner {
    override fun reason(
        inputModel: Model,
        catalogType: CatalogType,
    ): Model =
        when (catalogType) {
            CatalogType.CONCEPTS -> inputModel.conceptDeductions()
            CatalogType.DATASETS -> inputModel.fdkPrefix().datasetDeductions()
            CatalogType.DATASERVICES -> inputModel.dataServiceDeductions()
            CatalogType.INFORMATIONMODELS -> inputModel.informationModelDeductions()
            CatalogType.PUBLICSERVICES -> inputModel.servicesDeductions()
            else -> ModelFactory.createDefaultModel()
        }

    private fun Model.informationModelDeductions(): Model =
        ModelFactory.createInfModel(
            GenericRuleReasoner(Rule.parseRules(infoModelRules)),
            this,
        ).deductionsModel

    private fun Model.servicesDeductions(): Model =
        ModelFactory.createInfModel(
            GenericRuleReasoner(Rule.parseRules(serviceRules)),
            this,
        ).deductionsModel

    private fun Model.datasetDeductions(): Model =
        ModelFactory.createInfModel(
            GenericRuleReasoner(Rule.parseRules(datasetRules)),
            this,
        ).deductionsModel

    private fun Model.conceptDeductions(): Model =
        ModelFactory.createInfModel(
            GenericRuleReasoner(Rule.parseRules(conceptRules)),
            this,
        ).deductionsModel

    private fun Model.dataServiceDeductions(): Model =
        ModelFactory.createInfModel(
            GenericRuleReasoner(Rule.parseRules(dataServiceRules)),
            this,
        ).deductionsModel

}
