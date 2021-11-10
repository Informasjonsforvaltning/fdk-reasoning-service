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

        val fdkIdAndRecordURI = extractFDKIdAndRecordURI()

        return if (fdkIdAndRecordURI == null) null
        else Pair(
            fdkIdAndRecordURI.fdkId,
            eventModel.union(catalogRecordModel(fdkIdAndRecordURI.recordURI))
        )
    }

    private fun Model.recursiveAddNonEventOrServiceResource(resource: Resource, maxDepth: Int): Model {
        val newDepth = maxDepth - 1

        if (resourceShouldBeAdded(resource)) {
            add(resource.listProperties())

            if (newDepth > 0) {
                resource.listProperties().toList()
                    .filter { it.isResourceProperty() }
                    .forEach { recursiveAddNonEventOrServiceResource(it.resource, newDepth) }
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
