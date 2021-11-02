package no.fdk.fdk_reasoning_service.service

import no.fdk.fdk_reasoning_service.model.CatalogType
import no.fdk.fdk_reasoning_service.model.TurtleDBO
import no.fdk.fdk_reasoning_service.rdf.CPSV
import no.fdk.fdk_reasoning_service.rdf.CV
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.Resource
import org.apache.jena.riot.Lang
import org.apache.jena.vocabulary.RDF
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.findById
import org.springframework.stereotype.Service

private val LOGGER = LoggerFactory.getLogger(EventService::class.java)

@Service
class EventService(
    private val reasoningService: ReasoningService,
    private val eventMongoTemplate: MongoTemplate
) {

    fun reasonHarvestedEvents() {
        eventMongoTemplate.findById<TurtleDBO>("event-union-graph", "turtle")
            ?.let { parseRDFResponse(ungzip(it.turtle), Lang.TURTLE, "events") }
            ?.let { reasoningService.catalogReasoning(it, CatalogType.EVENTS) }
            ?. run { separateAndSaveEvents() }
            ?: run { LOGGER.error("harvested events not found", Exception()) }
    }

    private fun Model.separateAndSaveEvents() {
        eventMongoTemplate.save(createUnionDBO(), "fdkEvent")

        splitEventsFromRDF()
            .forEach { eventMongoTemplate.save(it.second.createDBO(it.first), "fdkEvent") }
        LOGGER.debug("reasoned events saved to db")
    }

    private fun Model.splitEventsFromRDF(): List<Pair<String, Model>> {
        val businessEvents = listResourcesWithProperty(RDF.type, CV.BusinessEvent)
            .toList()
            .mapNotNull { it.extractEventModel(nsPrefixMap) }

        val lifeEvents = listResourcesWithProperty(RDF.type, CV.LifeEvent)
            .toList()
            .mapNotNull { it.extractEventModel(nsPrefixMap) }

        return listOf(businessEvents, lifeEvents).flatten()
    }

    private fun Resource.extractEventModel(nsPrefixes: Map<String, String>): Pair<String, Model>? {
        var eventModel = listProperties().toModel()
        eventModel.setNsPrefixes(nsPrefixes)

        listProperties().toList()
            .filter { it.isResourceProperty() }
            .forEach {
                eventModel = eventModel.recursiveAddNonEventOrServiceResource(it.resource, 10)
            }

        val recordURI = catalogRecordURI()
        if (recordURI == null) {
            LOGGER.error("Unable to find record for $uri", Exception())
            return null
        }

        eventModel = eventModel.union(catalogRecordModel(recordURI))

        val fdkId = fdkId(recordURI)
        if (fdkId == null) {
            LOGGER.error("Unable to find fdkId for $recordURI", Exception())
            return null
        }

        return Pair(fdkId, eventModel)
    }

    private fun Resource.catalogRecordModel(recordURI: String): Model =
        ModelFactory.createDefaultModel()
            .recursiveAddNonEventOrServiceResource(model.getResource(recordURI), 4)

    private fun Model.recursiveAddNonEventOrServiceResource(resource: Resource, recursiveCount: Int): Model {
        val newCount = recursiveCount - 1

        if (resourceShouldBeAdded(resource)) {
            add(resource.listProperties())

            if (newCount > 0) {
                resource.listProperties().toList()
                    .filter { it.isResourceProperty() }
                    .forEach { recursiveAddNonEventOrServiceResource(it.resource, newCount) }
            }
        }

        return this
    }

    private fun Model.resourceShouldBeAdded(resource: Resource): Boolean {
        val types = resource.listProperties(RDF.type)
            .toList()
            .map { it.`object` }

        return when {
            types.contains(CPSV.PublicService) -> false
            types.contains(CV.BusinessEvent) -> false
            types.contains(CV.LifeEvent) -> false
            resource.uri == null -> true
            containsTriple("<${resource.uri}>", "a", "?o") -> false
            else -> true
        }
    }

}
