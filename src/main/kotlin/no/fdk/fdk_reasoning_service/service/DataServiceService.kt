package no.fdk.fdk_reasoning_service.service

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import no.fdk.fdk_reasoning_service.model.CatalogType
import no.fdk.fdk_reasoning_service.model.ExternalRDFData
import no.fdk.fdk_reasoning_service.model.HarvestReport
import no.fdk.fdk_reasoning_service.model.ReasoningReport
import no.fdk.fdk_reasoning_service.model.TurtleDBO
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.Resource
import org.apache.jena.riot.Lang
import org.apache.jena.vocabulary.DCAT
import org.apache.jena.vocabulary.RDF
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.findAll
import org.springframework.data.mongodb.core.findById
import org.springframework.stereotype.Service
import java.util.Date

private val LOGGER = LoggerFactory.getLogger(DataServiceService::class.java)

@Service
class DataServiceService(
    private val reasoningService: ReasoningService,
    private val dataServiceMongoTemplate: MongoTemplate
) {

    fun getAllDataServiceCatalogs(lang: Lang): String =
        dataServiceMongoTemplate.findById<TurtleDBO>(UNION_ID, "fdkCatalogs")
            ?.toRDF(lang)
            ?: ModelFactory.createDefaultModel().createRDFResponse(lang)

    fun getDataServiceCatalogById(id: String, lang: Lang): String? =
        dataServiceMongoTemplate.findById<TurtleDBO>(id, "fdkCatalogs")
            ?.toRDF(lang)

    fun getDataServiceById(id: String, lang: Lang): String? =
        dataServiceMongoTemplate.findById<TurtleDBO>(id, "fdkServices")
            ?.toRDF(lang)

    fun reasonReportedChanges(harvestReport: HarvestReport, rdfData: ExternalRDFData, start: Date): ReasoningReport =
        try {
            harvestReport.changedCatalogs
                .forEach { reasonCatalogServices(it.fdkId, rdfData) }

            ReasoningReport(
                id = harvestReport.id,
                url = harvestReport.url,
                dataType = CatalogType.DATASERVICES.toReportType(),
                harvestError = false,
                startTime = start.formatWithOsloTimeZone(),
                endTime = formatNowWithOsloTimeZone(),
                changedCatalogs = harvestReport.changedCatalogs,
                changedResources = harvestReport.changedResources,
                removedResources = harvestReport.removedResources
            )
        } catch (ex: Exception) {
            LOGGER.error("data service reasoning failed for ${harvestReport.url}", ex)
            ReasoningReport(
                id = harvestReport.id,
                url = harvestReport.url,
                dataType = CatalogType.DATASERVICES.toReportType(),
                harvestError = true,
                errorMessage = ex.message,
                startTime = start.formatWithOsloTimeZone(),
                endTime = formatNowWithOsloTimeZone()
            )
        }

    private fun reasonCatalogServices(catalogId: String, rdfData: ExternalRDFData) {
        dataServiceMongoTemplate.findById<TurtleDBO>(harvestedCatalogID(catalogId), "turtle")
            ?.let { parseRDFResponse(ungzip(it.turtle), Lang.TURTLE, "dataServices") }
            ?.let { reasoningService.catalogReasoning(it, CatalogType.DATASERVICES, rdfData) }
            ?.union(rdfData.ianaMediaTypes)
            ?.also { it.separateAndSaveDataServices() }
            ?: throw Exception("missing database data, harvest-reasoning was stopped")
    }

    fun updateUnion() {
        val catalogUnion = ModelFactory.createDefaultModel()

        dataServiceMongoTemplate.findAll<TurtleDBO>("fdkCatalogs")
            .filter { it.id != UNION_ID }
            .map { parseRDFResponse(ungzip(it.turtle), Lang.TURTLE, null) }
            .forEach { catalogUnion.add(it) }

        dataServiceMongoTemplate.save(catalogUnion.createUnionDBO(), "fdkCatalogs")
    }

    private fun Model.separateAndSaveDataServices() {
        splitDataServiceCatalogsFromRDF()
            .forEach { it.saveCatalogAndDataServiceModels() }
    }

    private fun CatalogAndDataServices.saveCatalogAndDataServiceModels() {
        dataServiceMongoTemplate.save(catalog.createDBO(fdkId), "fdkCatalogs")
        saveDataServiceModels()
    }

    private fun Model.splitDataServiceCatalogsFromRDF(): List<CatalogAndDataServices> =
        listResourcesWithProperty(RDF.type, DCAT.Catalog)
            .toList()
            .mapNotNull { catalogResource -> catalogResource.extractCatalog() }

    private fun Resource.extractCatalog(): CatalogAndDataServices? {
        val fdkIdAndRecordURI = extractFDKIdAndRecordURI()
        return if (fdkIdAndRecordURI == null) null
        else {
            val catalogServices: List<DataService> = listProperties(DCAT.service)
                .toList()
                .filter { it.isResourceProperty() }
                .filter { it.resource.hasProperty(RDF.type, DCAT.DataService) }
                .mapNotNull { concept -> concept.resource.extractDataService() }

            val catalogModelWithoutServices = listProperties().toModel()
            catalogModelWithoutServices.setNsPrefixes(model.nsPrefixMap)

            listProperties().toList()
                .filter { it.isResourceProperty() }
                .forEach {
                    if (it.predicate != DCAT.service) {
                        catalogModelWithoutServices.recursiveAddNonDataServiceResource(it.resource)
                    }
                }

            val servicesUnion = ModelFactory.createDefaultModel()
            catalogServices.forEach { servicesUnion.add(it.service) }

            catalogModelWithoutServices.add(catalogRecordModel(fdkIdAndRecordURI.recordURI))

            CatalogAndDataServices(
                fdkId = fdkIdAndRecordURI.fdkId,
                services = catalogServices,
                catalogWithoutServices = catalogModelWithoutServices,
                catalog = catalogModelWithoutServices.union(servicesUnion)
            )
        }
    }

    private fun Resource.extractDataService(): DataService? {
        val serviceModel = listProperties().toModel()
        serviceModel.setNsPrefixes(model.nsPrefixMap)

        listProperties().toList()
            .filter { it.isResourceProperty() }
            .forEach { serviceModel.recursiveAddNonDataServiceResource(it.resource) }

        val fdkIdAndRecordURI = extractFDKIdAndRecordURI()

        return if (fdkIdAndRecordURI == null) null
        else DataService(
            fdkId = fdkIdAndRecordURI.fdkId,
            service = serviceModel.union(catalogRecordModel(fdkIdAndRecordURI.recordURI))
        )
    }

    private fun Model.recursiveAddNonDataServiceResource(resource: Resource): Model {
        if (resourceShouldBeAdded(resource)) {
            add(resource.listProperties())

            resource.listProperties().toList()
                .filter { it.isResourceProperty() }
                .forEach { recursiveAddNonDataServiceResource(it.resource) }
        }

        return this
    }

    private fun Model.resourceShouldBeAdded(resource: Resource): Boolean {
        val types = resource.listProperties(RDF.type)
            .toList()
            .map { it.`object` }

        return when {
            types.contains(DCAT.DataService) -> false
            containsTriple("<${resource.uri}>", "a", "?o") -> false
            else -> true
        }
    }

    private fun harvestedCatalogID(fdkId: String): String =
        "catalog-$fdkId"

    private data class CatalogAndDataServices(
        val fdkId: String,
        val catalog: Model,
        val catalogWithoutServices: Model,
        val services: List<DataService>
    )

    private data class DataService(
        val fdkId: String,
        val service: Model
    )

    private fun CatalogAndDataServices.saveDataServiceModels() = runBlocking {
        val activitySemaphore = Semaphore(1)
        services.forEach {
            activitySemaphore.withPermit {
                it.service.union(catalogWithoutServices)
                    .createDBO(it.fdkId)
                    .let { dbo -> dataServiceMongoTemplate.save(dbo, "fdkServices") }
            }
        }
    }

}
