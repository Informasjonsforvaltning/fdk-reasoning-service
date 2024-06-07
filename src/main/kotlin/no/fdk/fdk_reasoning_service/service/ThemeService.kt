package no.fdk.fdk_reasoning_service.service

import no.fdk.fdk_reasoning_service.cache.ReferenceDataCache
import no.fdk.fdk_reasoning_service.model.CatalogType
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.reasoner.rulesys.GenericRuleReasoner
import org.apache.jena.reasoner.rulesys.Rule
import org.springframework.stereotype.Service

@Service
class ThemeService(
    private val referenceDataCache: ReferenceDataCache,
) : Reasoner {

    override fun reason(inputModel: Model, catalogType: CatalogType): Model =
        when (catalogType) {
            CatalogType.CONCEPTS -> ModelFactory.createDefaultModel()
            CatalogType.DATASERVICES -> themeReferenceData(inputModel)
            CatalogType.DATASETS -> datasetThemeReasoning(inputModel)
            CatalogType.EVENTS -> ModelFactory.createDefaultModel()
            CatalogType.INFORMATIONMODELS -> themeReferenceData(inputModel)
            CatalogType.PUBLICSERVICES -> themeReferenceData(inputModel)
        }

    private fun datasetThemeReasoning(inputModel: Model): Model {
        val matchingThemes = ModelFactory.createDefaultModel()
        matchingThemes.add(
            ModelFactory.createInfModel(
                GenericRuleReasoner(Rule.parseRules(dataThemesMatchingLOS)).bindSchema(referenceDataCache.los()),
                inputModel,
            ).deductionsModel
        )

        val datasetThemes = themeReferenceData(inputModel.union(matchingThemes))

        return datasetThemes.union(matchingThemes)
    }

    private fun themeReferenceData(inputModel: Model): Model {
        val referenceDataThemes = ModelFactory.createDefaultModel()

        referenceDataThemes.add(
            modelOfContainedReferenceData(
                inputModel,
                referenceDataCache.dataThemes()
            )
        )
        referenceDataThemes.add(
            modelOfContainedReferenceData(
                inputModel,
                referenceDataCache.los()
            )
        )
        referenceDataThemes.add(
            modelOfContainedReferenceData(
                inputModel,
                referenceDataCache.eurovocs()
            )
        )

        return referenceDataThemes
    }
}
