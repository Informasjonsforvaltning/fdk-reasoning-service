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
            CatalogType.DATASERVICES -> baseThemeReferenceData(inputModel)
            CatalogType.DATASETS -> datasetThemeReasoning(inputModel)
            CatalogType.EVENTS -> ModelFactory.createDefaultModel()
            CatalogType.INFORMATIONMODELS -> baseThemeReferenceData(inputModel)
            CatalogType.PUBLICSERVICES -> serviceThemeReferenceData(inputModel)
        }

    private fun datasetThemeReasoning(inputModel: Model): Model {
        val matchingThemes = ModelFactory.createDefaultModel()
        matchingThemes.add(
            ModelFactory.createInfModel(
                GenericRuleReasoner(Rule.parseRules(dataThemesMatchingLOS)).bindSchema(referenceDataCache.los()),
                inputModel,
            ).deductionsModel
        )

        val datasetThemes = datasetThemeReferenceData(inputModel.union(matchingThemes))

        return datasetThemes.union(matchingThemes)
    }

    private fun baseThemeReferenceData(inputModel: Model): Model {
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

        return referenceDataThemes
    }

    private fun datasetThemeReferenceData(inputModel: Model): Model {
        val referenceDataThemes = baseThemeReferenceData(inputModel)

        referenceDataThemes.add(
            modelOfContainedReferenceData(
                inputModel,
                referenceDataCache.mobilityThemes()
            )
        )

        return referenceDataThemes
    }

    private fun serviceThemeReferenceData(inputModel: Model): Model {
        val referenceDataThemes = baseThemeReferenceData(inputModel)

        referenceDataThemes.add(
            modelOfContainedReferenceData(
                inputModel,
                referenceDataCache.eurovocs()
            )
        )

        return referenceDataThemes
    }
}
