package no.fdk.fdk_reasoning_service.service

import no.fdk.fdk_reasoning_service.model.CatalogType
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.reasoner.rulesys.GenericRuleReasoner
import org.apache.jena.reasoner.rulesys.Rule
import org.springframework.stereotype.Service

@Service
class DeductionService : Reasoner {
    override fun reason(
        inputModel: Model,
        catalogType: CatalogType,
    ): Model =
        when (catalogType) {
            CatalogType.CONCEPTS -> inputModel.deduce(conceptRules)
            CatalogType.DATASETS -> inputModel.fdkPrefix().deduce(datasetRules)
            CatalogType.DATASERVICES -> inputModel.deduce(dataServiceRules)
            CatalogType.INFORMATIONMODELS -> inputModel.deduce(infoModelRules)
            CatalogType.PUBLICSERVICES -> inputModel.deduce(serviceRules)
            else -> ModelFactory.createDefaultModel()
        }

    private fun Model.deduce(rules: String): Model =
        ModelFactory.createInfModel(
            GenericRuleReasoner(Rule.parseRules(rules)),
            this,
        ).deductionsModel
}
