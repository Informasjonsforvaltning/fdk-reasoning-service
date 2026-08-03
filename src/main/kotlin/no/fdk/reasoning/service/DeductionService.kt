package no.fdk.reasoning.service

import no.fdk.reasoning.model.CatalogType
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
            CatalogType.CONCEPTS -> inputModel.deduce(CONCEPT_RULES)
            CatalogType.DATASETS -> inputModel.fdkPrefix().deduce(DATASET_RULES)
            CatalogType.DATASERVICES -> inputModel.deduce(DATA_SERVICE_RULES)
            CatalogType.INFORMATIONMODELS -> inputModel.deduce(INFO_MODEL_RULES)
            CatalogType.PUBLICSERVICES -> inputModel.deduce(SERVICE_RULES)
            else -> ModelFactory.createDefaultModel()
        }

    private fun Model.deduce(rules: String): Model =
        ModelFactory
            .createInfModel(
                GenericRuleReasoner(Rule.parseRules(rules)),
                this,
            ).deductionsModel
}
