package no.fdk.fdk_reasoning_service.service

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import no.fdk.fdk_reasoning_service.model.CatalogType
import no.fdk.fdk_reasoning_service.model.ExternalRDFData
import no.fdk.fdk_reasoning_service.model.HarvestReport
import no.fdk.fdk_reasoning_service.model.ReasoningReport
import no.fdk.fdk_reasoning_service.model.TurtleDBO
import no.fdk.fdk_reasoning_service.rdf.CPSV
import no.fdk.fdk_reasoning_service.rdf.CPSVNO
import no.fdk.fdk_reasoning_service.rdf.CV
import no.fdk.fdk_reasoning_service.rdf.DCATNO
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.RDFNode
import org.apache.jena.rdf.model.Resource
import org.apache.jena.riot.Lang
import org.apache.jena.vocabulary.DCAT
import org.apache.jena.vocabulary.DCTerms
import org.apache.jena.vocabulary.RDF
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.findAll
import org.springframework.data.mongodb.core.findById
import org.springframework.stereotype.Service
import java.util.Date

private val LOGGER = LoggerFactory.getLogger(EventService::class.java)

private enum class MongoDB(val collection: String) {
    REASONED_CATALOG("reasonedCatalog"),
    REASONED_SERVICE("reasonedPublicService"),
    HARVESTED_CATALOG("fdkCatalogTurtle")
}

@Service
class PublicServiceService(
    private val reasoningService: ReasoningService,
    private val publicServiceMongoTemplate: MongoTemplate
) {

    fun getAllPublicServices(lang: Lang): String =
        publicServiceMongoTemplate.findById<TurtleDBO>(UNION_ID, MongoDB.REASONED_SERVICE.collection)
            ?.toRDF(lang)
            ?: ModelFactory.createDefaultModel().createRDFResponse(lang)

    fun getPublicServiceById(id: String, lang: Lang): String? =
        publicServiceMongoTemplate.findById<TurtleDBO>(id, MongoDB.REASONED_SERVICE.collection)
            ?.toRDF(lang)

    fun getAllCatalogs(lang: Lang): String =
        publicServiceMongoTemplate.findById<TurtleDBO>(UNION_ID, MongoDB.REASONED_CATALOG.collection)
            ?.toRDF(lang)
            ?: ModelFactory.createDefaultModel().createRDFResponse(lang)

    fun getCatalogById(id: String, lang: Lang): String? =
        publicServiceMongoTemplate.findById<TurtleDBO>(id, MongoDB.REASONED_CATALOG.collection)
            ?.toRDF(lang)

    fun reasonReportedChanges(harvestReport: HarvestReport, rdfData: ExternalRDFData, start: Date): ReasoningReport =
        try {
            harvestReport.changedCatalogs
                .forEach { reasonServiceCatalog(it.fdkId, rdfData) }

            ReasoningReport(
                id = harvestReport.id,
                url = harvestReport.url,
                dataType = CatalogType.PUBLICSERVICES.toReportType(),
                harvestError = false,
                startTime = start.formatWithOsloTimeZone(),
                endTime = formatNowWithOsloTimeZone(),
                changedCatalogs = harvestReport.changedCatalogs,
                changedResources = harvestReport.changedResources,
                removedResources = harvestReport.removedResources
            )
        } catch (ex: Exception) {
            LOGGER.error("event reasoning failed for ${harvestReport.url}", ex)
            ReasoningReport(
                id = harvestReport.id,
                url = harvestReport.url,
                dataType = CatalogType.PUBLICSERVICES.toReportType(),
                harvestError = true,
                errorMessage = ex.message,
                startTime = start.formatWithOsloTimeZone(),
                endTime = formatNowWithOsloTimeZone()
            )
        }

    private fun reasonServiceCatalog(catalogId: String, rdfData: ExternalRDFData) {
        publicServiceMongoTemplate.findById<TurtleDBO>(catalogId, MongoDB.HARVESTED_CATALOG.collection)
            ?.let { parseRDFResponse(ungzip(it.turtle), Lang.TURTLE, "public-services") }
            ?.let { reasoningService.catalogReasoning(it, CatalogType.PUBLICSERVICES, rdfData) }
            ?.union(rdfData.toModel())
            ?.also { it.separateAndSavePublicServices() }
            ?: throw Exception("missing database data, harvest-reasoning was stopped")
    }

    private fun ExternalRDFData.toModel(): Model {
        val m = ModelFactory.createDefaultModel()
        m.add(selectedThemeTriples())
        m.add(linguisticSystems)
        return m
    }

    fun updateUnion() {
        val catalogUnion = ModelFactory.createDefaultModel()
        val serviceUnion = ModelFactory.createDefaultModel()

        publicServiceMongoTemplate.findAll<TurtleDBO>(MongoDB.REASONED_CATALOG.collection)
            .filter { it.id != UNION_ID }
            .map { parseRDFResponse(ungzip(it.turtle), Lang.TURTLE, null) }
            .forEach { catalogUnion.add(it) }

        publicServiceMongoTemplate.findAll<TurtleDBO>(MongoDB.REASONED_SERVICE.collection)
            .filter { it.id != UNION_ID }
            .map { parseRDFResponse(ungzip(it.turtle), Lang.TURTLE, null) }
            .forEach { serviceUnion.add(it) }

        publicServiceMongoTemplate.save(catalogUnion.createUnionDBO(), MongoDB.REASONED_CATALOG.collection)
        publicServiceMongoTemplate.save(serviceUnion.createUnionDBO(), MongoDB.REASONED_SERVICE.collection)
    }

    private fun Model.separateAndSavePublicServices() {
        splitPublicServiceCatalogsFromRDF()
            .forEach { it.saveCatalogAndPublicServiceModels() }
    }

    private fun CatalogAndPublicServices.saveCatalogAndPublicServiceModels() {
        publicServiceMongoTemplate.save(catalog.createDBO(fdkId), MongoDB.REASONED_CATALOG.collection)
        savePublicServiceModels()
    }

    private fun Model.splitPublicServiceCatalogsFromRDF(): List<CatalogAndPublicServices> =
        listResourcesWithProperty(RDF.type, DCAT.Catalog)
            .toList()
            .mapNotNull { catalogResource -> catalogResource.extractCatalog() }

    private fun Resource.extractCatalog(): CatalogAndPublicServices? {
        val fdkIdAndRecordURI = extractFDKIdAndRecordURI()
        return if (fdkIdAndRecordURI == null) null
        else {
            val catalogServices: List<PublicService> = listProperties(DCATNO.containsService)
                .toList()
                .filter { it.isResourceProperty() }
                .filter { it.resource.hasPublicServiceType() }
                .mapNotNull { it.resource.extractPublicService() }

            val catalogModelWithoutServices = listProperties().toModel()
            catalogModelWithoutServices.setNsPrefixes(model.nsPrefixMap)

            listProperties().toList()
                .filter { it.isResourceProperty() }
                .forEach {
                    if (it.predicate != DCAT.service) {
                        catalogModelWithoutServices.recursiveAddNonPublicServiceResource(it.resource)
                    }
                }

            val servicesUnion = ModelFactory.createDefaultModel()
            catalogServices.forEach { servicesUnion.add(it.service) }

            catalogModelWithoutServices.add(catalogRecordModel(fdkIdAndRecordURI.recordURI))

            CatalogAndPublicServices(
                fdkId = fdkIdAndRecordURI.fdkId,
                services = catalogServices,
                catalogWithoutServices = catalogModelWithoutServices,
                catalog = catalogModelWithoutServices.union(servicesUnion)
            )
        }
    }

    private fun Resource.extractPublicService(): PublicService? {
        val serviceModel = listProperties().toModel()
        serviceModel.setNsPrefixes(model.nsPrefixMap)

        listProperties().toList()
            .filter { it.isResourceProperty() }
            .forEach { serviceModel.recursiveAddNonPublicServiceResource(it.resource) }

        val fdkIdAndRecordURI = extractFDKIdAndRecordURI()

        return if (fdkIdAndRecordURI == null) null
        else PublicService(
            fdkId = fdkIdAndRecordURI.fdkId,
            service = serviceModel.union(catalogRecordModel(fdkIdAndRecordURI.recordURI))
        )
    }

    private fun Model.recursiveAddNonPublicServiceResource(resource: Resource): Model {
        val types = resource.listProperties(RDF.type)
            .toList()
            .map { it.`object` }

        if (resourceShouldBeAdded(resource, types)) {
            add(resource.listProperties())

            resource.listProperties().toList()
                .filter { it.isResourceProperty() }
                .forEach { recursiveAddNonPublicServiceResource(it.resource) }
        }

        if (types.contains(CV.Participation)) addAgentsAssociatedWithParticipation(resource)

        return this
    }

    private fun Model.addAgentsAssociatedWithParticipation(resource: Resource): Model {
        resource.model
            .listResourcesWithProperty(RDF.type, DCTerms.Agent)
            .toList()
            .filter { it.hasProperty(CV.playsRole, resource) }
            .forEach { codeElement ->
                add(codeElement.listProperties())

                codeElement.listProperties().toList()
                    .filter { it.isResourceProperty() }
                    .forEach { add(it.resource.listProperties()) }
            }

        return this
    }

    private fun Model.resourceShouldBeAdded(resource: Resource, types: List<RDFNode>): Boolean =
        when {
            types.contains(CPSV.PublicService) -> false
            types.contains(CPSVNO.Service) -> false
            types.contains(CV.Event) -> false
            types.contains(CV.BusinessEvent) -> false
            types.contains(CV.LifeEvent) -> false
            !resource.isURIResource -> true
            containsTriple("<${resource.uri}>", "a", "?o") -> false
            else -> true
        }

    private fun Resource.hasPublicServiceType(): Boolean =
        hasProperty(RDF.type, CPSV.PublicService) || hasProperty(RDF.type, CPSVNO.Service)

    private data class CatalogAndPublicServices(
        val fdkId: String,
        val catalog: Model,
        val catalogWithoutServices: Model,
        val services: List<PublicService>
    )

    private data class PublicService(
        val fdkId: String,
        val service: Model
    )

    private fun CatalogAndPublicServices.savePublicServiceModels() = runBlocking {
        val activitySemaphore = Semaphore(1)
        services.forEach {
            activitySemaphore.withPermit {
                it.service.union(catalogWithoutServices)
                    .createDBO(it.fdkId)
                    .let { dbo -> publicServiceMongoTemplate.save(dbo, MongoDB.REASONED_SERVICE.collection) }
            }
        }
    }

}
