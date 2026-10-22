package no.fdk.reasoning.service

import no.fdk.reasoning.cache.ReferenceDataCache
import no.fdk.reasoning.model.CatalogType
import no.fdk.reasoning.rdf.FDK
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.reasoner.rulesys.GenericRuleReasoner
import org.apache.jena.reasoner.rulesys.Rule
import org.springframework.stereotype.Service

@Service
class DeductionService(private val referenceDataCache: ReferenceDataCache) : Reasoner {
    override fun reason(inputModel: Model, catalogType: CatalogType): Model = when (catalogType) {
        CatalogType.CONCEPTS -> inputModel.deduce(CONCEPT_RULES)
        CatalogType.DATASETS -> inputModel.fdkPrefix().deduce(datasetRules(napThemes(referenceDataCache.los())))
        CatalogType.DATASERVICES -> inputModel.deduce(DATA_SERVICE_RULES)
        CatalogType.INFORMATIONMODELS -> inputModel.deduce(INFO_MODEL_RULES)
        CatalogType.PUBLICSERVICES -> inputModel.deduce(SERVICE_RULES)
        else -> ModelFactory.createDefaultModel()
    }

    private fun napThemes(los: Model): Set<String> = los.listStatements(null, FDK.themePath, null as String?)
        .toList()
        .filter { it.subject.isURIResource && it.`object`.isLiteral && isNapLosThemePath(it.string) }
        .map { it.subject.uri }
        .toSet() + NAP_DATA_THEME

    private fun isNapLosThemePath(path: String): Boolean = path.split("/").first() == NAP_LOS_THEME_PATH

    private fun Model.deduce(rules: String): Model = ModelFactory
        .createInfModel(
            GenericRuleReasoner(Rule.parseRules(rules)),
            this,
        ).deductionsModel
}
