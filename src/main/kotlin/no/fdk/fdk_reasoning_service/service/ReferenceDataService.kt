package no.fdk.fdk_reasoning_service.service

import no.fdk.fdk_reasoning_service.cache.ReferenceDataCache
import no.fdk.fdk_reasoning_service.model.CatalogType
import no.fdk.fdk_reasoning_service.rdf.FDK
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.Resource
import org.springframework.stereotype.Service

@Service
class ReferenceDataService(
    private val referenceDataCache: ReferenceDataCache
) {
    fun referenceDataModel(inputModel: Model, catalogType: CatalogType): Model {
        val m = ModelFactory.createDefaultModel()

        catalogType.completeReferenceDataModel()
            .listSubjects()
            .toList()
            .filter { inputModel.containsTriple("?s", "?p", "<${it.uri}>") }
            .forEach { it.recursiveAddReferenceCodeProperties(m) }

        return m
    }

    private fun Resource.recursiveAddReferenceCodeProperties(m: Model) {
        listProperties()
            .toList()
            .filter { !m.contains(it) }
            .also { m.add(it) }
            .filter { it.isResourceProperty() }
            .map { it.resource }
            .filter { it.isAnon }
            .forEach { it.recursiveAddReferenceCodeProperties(m) }
    }

    private fun CatalogType.completeReferenceDataModel(): Model =
        when (this) {
            CatalogType.CONCEPTS -> conceptReferenceData()
            CatalogType.DATASERVICES -> dataServiceReferenceData()
            CatalogType.DATASETS -> datasetReferenceData()
            CatalogType.EVENTS -> ModelFactory.createDefaultModel()
            CatalogType.INFORMATIONMODELS -> informationModelReferenceData()
            CatalogType.PUBLICSERVICES -> serviceReferenceData()
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

    private fun dataServiceReferenceData(): Model {
        val ianaMediaTypes = referenceDataCache.ianaMediaTypes()
        if (ianaMediaTypes.isEmpty) throw Exception("IANA media types are missing in reference data cache")

        val fileTypes = referenceDataCache.fileTypes()
        if (fileTypes.isEmpty) throw Exception("File types are missing in reference data cache")

        val m = ModelFactory.createDefaultModel()
        m.add(ianaMediaTypes)
        m.add(fileTypes)
        return m
    }

    private fun datasetReferenceData(): Model {
        val ianaMediaTypes = referenceDataCache.ianaMediaTypes()
        if (ianaMediaTypes.isEmpty) throw Exception("IANA media types are missing in reference data cache")

        val fileTypes = referenceDataCache.fileTypes()
        if (fileTypes.isEmpty) throw Exception("File types are missing in reference data cache")

        val openLicenses = referenceDataCache.openLicenses()
        if (openLicenses.isEmpty) throw Exception("Open licenses are missing in reference data cache")

        val linguisticSystems = referenceDataCache.linguisticSystems()
        if (linguisticSystems.isEmpty) throw Exception("Linguistic systems are missing in reference data cache")

        val locations = referenceDataCache.locations()
        if (locations.isEmpty) throw Exception("Locations are missing in reference data cache")

        val accessRights = referenceDataCache.accessRights()
        if (accessRights.isEmpty) throw Exception("Access rights are missing in reference data cache")

        val frequencies = referenceDataCache.frequencies()
        if (frequencies.isEmpty) throw Exception("Frequencies are missing in reference data cache")

        val provenance = referenceDataCache.provenance()
        if (provenance.isEmpty) throw Exception("Provenance are missing in reference data cache")

        val m = ModelFactory.createDefaultModel()
        m.add(selectedThemeTriples())
        m.add(ianaMediaTypes)
        m.add(fileTypes)
        m.add(openLicenses)
        m.add(linguisticSystems)
        m.add(locations)
        m.add(accessRights)
        m.add(frequencies)
        m.add(provenance)
        return m
    }


    private fun informationModelReferenceData(): Model {
        val openLicenses = referenceDataCache.openLicenses()
        if (openLicenses.isEmpty) throw Exception("Open licenses are missing in reference data cache")

        val linguisticSystems = referenceDataCache.linguisticSystems()
        if (linguisticSystems.isEmpty) throw Exception("Linguistic systems are missing in reference data cache")

        val locations = referenceDataCache.locations()
        if (locations.isEmpty) throw Exception("Locations are missing in reference data cache")

        val m = ModelFactory.createDefaultModel()
        m.add(selectedThemeTriples())
        m.add(openLicenses)
        m.add(linguisticSystems)
        m.add(locations)
        return m
    }

    private fun serviceReferenceData(): Model {
        val linguisticSystems = referenceDataCache.linguisticSystems()
        if (linguisticSystems.isEmpty) throw Exception("Linguistic systems are missing in reference data cache")

        val publisherTypes = referenceDataCache.publisherTypes()
        if (publisherTypes.isEmpty) throw Exception("Publisher types are missing in reference data cache")

        val admsStatuses = referenceDataCache.admsStatuses()
        if (admsStatuses.isEmpty) throw Exception("ADMS statuses are missing in reference data cache")

        val roleTypes = referenceDataCache.roleTypes()
        if (roleTypes.isEmpty) throw Exception("Role types are missing in reference data cache")

        val evidenceTypes = referenceDataCache.evidenceTypes()
        if (evidenceTypes.isEmpty) throw Exception("Evidence types are missing in reference data cache")

        val channelTypes = referenceDataCache.channelTypes()
        if (channelTypes.isEmpty) throw Exception("Channel types are missing in reference data cache")

        val mainActivities = referenceDataCache.mainActivities()
        if (mainActivities.isEmpty) throw Exception("Main activities are missing in reference data cache")

        val weekDays = referenceDataCache.weekDays()
        if (weekDays.isEmpty) throw Exception("Week days are missing in reference data cache")

        val m = ModelFactory.createDefaultModel()
        m.add(selectedThemeTriples())
        m.add(linguisticSystems)
        m.add(publisherTypes)
        m.add(admsStatuses)
        m.add(roleTypes)
        m.add(evidenceTypes)
        m.add(channelTypes)
        m.add(mainActivities)
        m.add(weekDays)
        return m
    }

    private fun selectedThemeTriples(): Model {
        val losData = referenceDataCache.los()
        if (losData.isEmpty) throw Exception("LOS are missing in reference data cache")

        val eurovocs = referenceDataCache.eurovocs()
        if (eurovocs.isEmpty) throw Exception("EUROVOCS are missing in reference data cache")

        val dataThemes = referenceDataCache.dataThemes()
        if (dataThemes.isEmpty) throw Exception("Data themes are missing in reference data cache")

        return ModelFactory.createDefaultModel()
            .add(losData.listStatements().filterKeep { s -> s.predicate == FDK.themePath }.toList())
            .add(eurovocs.listStatements().filterKeep { s -> s.predicate == FDK.themePath }.toList())
            .add(dataThemes.listStatements().filterKeep { s -> s.predicate == FDK.themePath }.toList())
    }

}
