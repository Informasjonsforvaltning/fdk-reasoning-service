package no.fdk.fdk_reasoning_service.service

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
import org.apache.jena.rdf.model.Resource
import org.apache.jena.rdf.model.Statement
import org.apache.jena.riot.Lang
import org.apache.jena.vocabulary.DCAT
import org.apache.jena.vocabulary.RDF
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.findAll
import org.springframework.data.mongodb.core.findById
import org.springframework.stereotype.Service
import java.util.Date

private val LOGGER = LoggerFactory.getLogger(EventService::class.java)

@Service
class EventService(
    private val reasoningService: ReasoningService,
    private val eventMongoTemplate: MongoTemplate
) {

    private enum class MongoDB(val collection: String) {
        REASONED_CATALOG("reasonedCatalog"),
        REASONED_EVENT("reasonedEvent"),
        HARVESTED_CATALOG("fdkCatalogTurtle")
    }

    fun getAllEvents(lang: Lang): String =
        eventMongoTemplate.findById<TurtleDBO>(UNION_ID, MongoDB.REASONED_EVENT.collection)
            ?.toRDF(lang)
            ?: ModelFactory.createDefaultModel().createRDFResponse(lang)

    fun getEventById(id: String, lang: Lang): String? =
        eventMongoTemplate.findById<TurtleDBO>(id, MongoDB.REASONED_EVENT.collection)
            ?.toRDF(lang)

    fun getAllCatalogs(lang: Lang): String =
        eventMongoTemplate.findById<TurtleDBO>(UNION_ID, MongoDB.REASONED_CATALOG.collection)
            ?.toRDF(lang)
            ?: ModelFactory.createDefaultModel().createRDFResponse(lang)

    fun getCatalogById(id: String, lang: Lang): String? =
        eventMongoTemplate.findById<TurtleDBO>(id, MongoDB.REASONED_CATALOG.collection)
            ?.toRDF(lang)

    fun reasonReportedChanges(harvestReport: HarvestReport, rdfData: ExternalRDFData, start: Date): ReasoningReport =
        try {
            harvestReport.changedCatalogs
                .forEach { reasonEventCatalog(it.fdkId, rdfData) }

            ReasoningReport(
                id = harvestReport.id,
                url = harvestReport.url,
                dataType = CatalogType.EVENTS.toReportType(),
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
                dataType = CatalogType.EVENTS.toReportType(),
                harvestError = true,
                errorMessage = ex.message,
                startTime = start.formatWithOsloTimeZone(),
                endTime = formatNowWithOsloTimeZone()
            )
        }

    private fun reasonEventCatalog(catalogId: String, rdfData: ExternalRDFData) {
        eventMongoTemplate.findById<TurtleDBO>(catalogId, MongoDB.HARVESTED_CATALOG.collection)
            ?.let { parseRDFResponse(ungzip(it.turtle), Lang.TURTLE, MongoDB.HARVESTED_CATALOG.collection) }
            ?.let { reasoningService.catalogReasoning(it, CatalogType.EVENTS, rdfData) }
            ?.also { it.seperateAndSaveEvents() }
            ?: throw Exception("missing database data, harvest-reasoning was stopped")
    }

    fun updateUnion() {
        val catalogUnion = ModelFactory.createDefaultModel()
        val eventUnion = ModelFactory.createDefaultModel()

        eventMongoTemplate.findAll<TurtleDBO>(MongoDB.REASONED_CATALOG.collection)
            .filter { it.id != UNION_ID }
            .map { parseRDFResponse(ungzip(it.turtle), Lang.TURTLE, null) }
            .forEach { catalogUnion.add(it) }

        eventMongoTemplate.findAll<TurtleDBO>(MongoDB.REASONED_EVENT.collection)
            .filter { it.id != UNION_ID }
            .map { parseRDFResponse(ungzip(it.turtle), Lang.TURTLE, null) }
            .forEach { eventUnion.add(it) }

        eventMongoTemplate.save(catalogUnion.createUnionDBO(), MongoDB.REASONED_CATALOG.collection)
        eventMongoTemplate.save(eventUnion.createUnionDBO(), MongoDB.REASONED_EVENT.collection)
    }

    private fun Model.seperateAndSaveEvents() {
        splitEventCatalogsFromRDF()
            .forEach { it.saveCatalogAndEventModels() }
    }

    private fun CatalogAndEvents.saveCatalogAndEventModels() {
        eventMongoTemplate.save(catalog.createDBO(fdkId), MongoDB.REASONED_CATALOG.collection)

        events.map { it.copy(event = catalogWithoutEvents.union(it.event)) }
            .forEach { eventMongoTemplate.save(it.event.createDBO(it.fdkId), MongoDB.REASONED_EVENT.collection) }
    }

    private fun Model.splitEventCatalogsFromRDF(): List<CatalogAndEvents> =
        listResourcesWithProperty(RDF.type, DCAT.Catalog)
            .toList()
            .mapNotNull { catalogResource -> catalogResource.extractCatalog() }

    private fun Resource.extractCatalog(): CatalogAndEvents? {
        val fdkIdAndRecordURI = extractFDKIdAndRecordURI()
        return if (fdkIdAndRecordURI == null) null
        else {
            val catalogEvents: List<Event> = listProperties(DCATNO.containsEvent)
                .toList()
                .filter { it.isResourceProperty() }
                .filter { it.resource.hasEventType() }
                .mapNotNull { it.resource.extractEvent() }
            val catalogModelWithoutEvents = ModelFactory.createDefaultModel()
            catalogModelWithoutEvents.setNsPrefixes(model.nsPrefixMap)

            listProperties()
                .toList()
                .forEach { catalogModelWithoutEvents.addCatalogProperties(it) }

            val eventsUnion = ModelFactory.createDefaultModel()
            catalogEvents.forEach { eventsUnion.add(it.event) }

            catalogModelWithoutEvents.add(catalogRecordModel(fdkIdAndRecordURI.recordURI))

            CatalogAndEvents(
                fdkId = fdkIdAndRecordURI.fdkId,
                events = catalogEvents,
                catalogWithoutEvents = catalogModelWithoutEvents,
                catalog = catalogModelWithoutEvents.union(eventsUnion)
            )
        }
    }

    private fun Model.addCatalogProperties(property: Statement): Model =
        when {
            property.predicate != DCATNO.containsEvent && property.isResourceProperty() ->
                add(property).recursiveAddNonEventResource(property.resource)

            property.predicate != DCATNO.containsEvent -> add(property)
            property.isResourceProperty() && property.resource.isURIResource -> add(property)
            else -> this
        }

    private fun Resource.extractEvent(): Event? {
        val eventModel = listProperties().toModel()
        eventModel.setNsPrefixes(model.nsPrefixMap)

        listProperties().toList()
            .filter { it.isResourceProperty() }
            .forEach { eventModel.recursiveAddNonEventResource(it.resource) }

        val fdkIdAndRecordURI = extractFDKIdAndRecordURI()

        return if (fdkIdAndRecordURI == null) null
        else Event(
            fdkId = fdkIdAndRecordURI.fdkId,
            event = eventModel.union(catalogRecordModel(fdkIdAndRecordURI.recordURI))
        )
    }

    private fun Model.recursiveAddNonEventResource(resource: Resource): Model {
        if (resourceShouldBeAdded(resource)) {
            add(resource.listProperties())

            resource.listProperties().toList()
                .filter { it.isResourceProperty() }
                .forEach { recursiveAddNonEventResource(it.resource) }
        }

        return this
    }

    private fun Model.resourceShouldBeAdded(resource: Resource): Boolean {
        val types = resource.listProperties(RDF.type)
            .toList()
            .map { it.`object` }

        return when {
            types.contains(CPSV.PublicService) -> false
            types.contains(CPSVNO.Service) -> false
            types.contains(CV.Event) -> false
            types.contains(CV.BusinessEvent) -> false
            types.contains(CV.LifeEvent) -> false
            !resource.isURIResource -> true
            containsTriple("<${resource.uri}>", "a", "?o") -> false
            else -> true
        }
    }

    private fun Resource.hasEventType(): Boolean =
        hasProperty(RDF.type, CV.Event)
                || hasProperty(RDF.type, CV.BusinessEvent)
                || hasProperty(RDF.type, CV.LifeEvent)

    private data class CatalogAndEvents(
        val fdkId: String,
        val catalog: Model,
        val catalogWithoutEvents: Model,
        val events: List<Event>
    )

    private data class Event(
        val fdkId: String,
        val event: Model
    )

}
