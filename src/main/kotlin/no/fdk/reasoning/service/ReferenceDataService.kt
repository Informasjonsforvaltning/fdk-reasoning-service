package no.fdk.reasoning.service

import no.fdk.reasoning.cache.ReferenceDataCache
import no.fdk.reasoning.model.CatalogType
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.springframework.stereotype.Service

@Service
class ReferenceDataService(
    private val referenceDataCache: ReferenceDataCache,
) : Reasoner {
    override fun reason(
        inputModel: Model,
        catalogType: CatalogType,
    ): Model =
        modelOfContainedReferenceData(
            inputModel,
            catalogType.completeReferenceDataModel(),
        )

    private fun CatalogType.completeReferenceDataModel(): Model =
        when (this) {
            CatalogType.CONCEPTS -> conceptReferenceData()
            CatalogType.DATASERVICES -> dataServiceReferenceData()
            CatalogType.DATASETS -> datasetReferenceData()
            CatalogType.EVENTS -> ModelFactory.createDefaultModel()
            CatalogType.INFORMATIONMODELS -> informationModelReferenceData()
            CatalogType.PUBLICSERVICES -> serviceReferenceData()
        }

    private fun conceptReferenceData(): Model =
        requireAndUnion(
            "Concept statuses" to referenceDataCache.conceptStatuses(),
            "Concept subjects" to referenceDataCache.conceptSubjects(),
        )

    private fun dataServiceReferenceData(): Model =
        requireAndUnion(
            "IANA media types" to referenceDataCache.ianaMediaTypes(),
            "File types" to referenceDataCache.fileTypes(),
            "Licences" to referenceDataCache.licences(),
        )

    private fun datasetReferenceData(): Model =
        requireAndUnion(
            "IANA media types" to referenceDataCache.ianaMediaTypes(),
            "File types" to referenceDataCache.fileTypes(),
            "Licences" to referenceDataCache.licences(),
            "Languages" to referenceDataCache.languages(),
            "Locations" to referenceDataCache.locations(),
            "Access rights" to referenceDataCache.accessRights(),
            "Frequencies" to referenceDataCache.frequencies(),
            "Provenance" to referenceDataCache.provenance(),
            "Dataset types" to referenceDataCache.datasetTypes(),
            "Distribution statuses" to referenceDataCache.distributionStatuses(),
            "Mobility data standards" to referenceDataCache.mobilityDataStandards(),
            "Mobility conditions" to referenceDataCache.mobilityConditions(),
            "High value categories" to referenceDataCache.highValueCategories(),
            "Quality dimensions" to referenceDataCache.qualityDimensions(),
            "Legal resource types" to referenceDataCache.legalResourceTypes(),
            "Geonames" to referenceDataCache.geonames(),
            "EU continents" to referenceDataCache.euContinents(),
            "EU countries" to referenceDataCache.euCountries(),
        )

    private fun informationModelReferenceData(): Model =
        requireAndUnion(
            "Licences" to referenceDataCache.licences(),
            "Languages" to referenceDataCache.languages(),
            "Locations" to referenceDataCache.locations(),
        )

    private fun serviceReferenceData(): Model =
        requireAndUnion(
            "Languages" to referenceDataCache.languages(),
            "Locations" to referenceDataCache.locations(),
            "Publisher types" to referenceDataCache.publisherTypes(),
            "ADMS statuses" to referenceDataCache.admsStatuses(),
            "Role types" to referenceDataCache.roleTypes(),
            "Evidence types" to referenceDataCache.evidenceTypes(),
            "Channel types" to referenceDataCache.channelTypes(),
            "Main activities" to referenceDataCache.mainActivities(),
            "Week days" to referenceDataCache.weekDays(),
            "Geonames" to referenceDataCache.geonames(),
            "EU continents" to referenceDataCache.euContinents(),
            "EU countries" to referenceDataCache.euCountries(),
        )

    private fun requireAndUnion(vararg sources: Pair<String, Model>): Model {
        val m = ModelFactory.createDefaultModel()
        sources.forEach { (label, model) ->
            if (model.isEmpty) throw Exception("$label are missing in reference data cache")
            m.add(model)
        }
        return m
    }
}
