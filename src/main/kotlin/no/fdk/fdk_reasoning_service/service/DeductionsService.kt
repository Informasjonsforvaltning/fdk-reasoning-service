package no.fdk.fdk_reasoning_service.service

import no.fdk.fdk_reasoning_service.cache.ReferenceDataCache
import no.fdk.fdk_reasoning_service.model.CatalogType
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.reasoner.rulesys.GenericRuleReasoner
import org.apache.jena.reasoner.rulesys.Rule
import org.springframework.stereotype.Service

@Service
class DeductionService(
    private val referenceDataCache: ReferenceDataCache
): Reasoner {

    override fun reason(inputModel: Model, catalogType: CatalogType): Model =
        when (catalogType) {
            CatalogType.CONCEPTS -> ModelFactory.createInfModel(
                GenericRuleReasoner(Rule.parseRules(conceptRules)),
                inputModel
            ).deductionsModel

            CatalogType.DATASETS -> inputModel.fdkPrefix().datasetDeductions()
            CatalogType.DATASERVICES -> ModelFactory.createInfModel(
                GenericRuleReasoner(Rule.parseRules(dataServiceRules)),
                inputModel
            ).deductionsModel

            CatalogType.INFORMATIONMODELS -> inputModel.informationModelDeductions()
            CatalogType.PUBLICSERVICES -> inputModel.servicesDeductions()
            else -> ModelFactory.createDefaultModel()
        }

    private fun Model.informationModelDeductions(): Model {
        val deductions = ModelFactory.createInfModel(
            GenericRuleReasoner(Rule.parseRules(infoModelRules)),
            this
        ).deductionsModel

        val themeLabelDeductions = union(deductions).themePrefLabelDeductions()

        return deductions.union(themeLabelDeductions)
    }

    private fun Model.servicesDeductions(): Model {
        val deductions = ModelFactory.createInfModel(
            GenericRuleReasoner(Rule.parseRules(serviceRules)),
            this
        ).deductionsModel

        val themeLabelDeductions = union(deductions).themePrefLabelDeductions()

        return deductions.union(themeLabelDeductions)
    }

    private fun Model.datasetDeductions(): Model {
        val losData = referenceDataCache.los()
        if (losData.isEmpty) throw Exception("Reference data cache missing themes")

        val deductions = ModelFactory.createInfModel(
            GenericRuleReasoner(Rule.parseRules(datasetRules)).bindSchema(losData),
            this
        ).deductionsModel

        val themeLabelDeductions = union(deductions).themePrefLabelDeductions()

        return deductions.union(themeLabelDeductions)
    }

    private fun Model.themePrefLabelDeductions(): Model {
        val losData = referenceDataCache.los()
        val dataThemes = referenceDataCache.dataThemes()
        val eurovocs = referenceDataCache.eurovocs()
        if (losData.isEmpty || dataThemes.isEmpty || eurovocs.isEmpty) throw Exception("Reference data cache missing themes")

        // tag themes that's missing prefLabel in input model, to easier add all lang-options for relevant themes
        val themesMissingLabels = ModelFactory.createInfModel(
            GenericRuleReasoner(Rule.parseRules(tagThemesMissingLabel)),
            this
        ).deductionsModel

        // get theme labels as dct:title, so as not to confuse reasoner when adding them as prefLabel to input model later
        val themeTitles = ModelFactory.createInfModel(
            GenericRuleReasoner(Rule.parseRules(labelToTitle)),
            losData.union(eurovocs).union(dataThemes)
        ).deductionsModel

        // add prefLabel from themeTitles for themes tagged as missing label
        return ModelFactory.createInfModel(
            GenericRuleReasoner(Rule.parseRules(addThemeTitles)).bindSchema(themeTitles.union(themesMissingLabels)),
            this
        ).deductionsModel
    }

}

