package no.fdk.fdk_reasoning_service.service

import no.fdk.fdk_reasoning_service.cache.ReferenceDataCache
import no.fdk.fdk_reasoning_service.model.CatalogType
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.springframework.stereotype.Service

@Service
class ReferenceDataService(
    private val referenceDataCache: ReferenceDataCache
) {
    fun referenceDataModel(catalogType: CatalogType): Model =
        when (catalogType) {
            CatalogType.CONCEPTS -> conceptReferenceData()
            else -> ModelFactory.createDefaultModel()
        }

    private fun conceptReferenceData(): Model {
        val conceptStatuses = referenceDataCache.conceptStatuses()
        if (conceptStatuses.isEmpty) throw Exception("Concept statuses are missing in reference data cache")

        val conceptSubjects = referenceDataCache.conceptSubjects()
        if (conceptSubjects.isEmpty) throw Exception("Concept subjects are missing in reference data cache")

        val m = ModelFactory.createDefaultModel()
        m.add(conceptStatuses)
        m.add(conceptSubjects)
        return m
    }
}
