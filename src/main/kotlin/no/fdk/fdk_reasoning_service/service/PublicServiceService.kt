package no.fdk.fdk_reasoning_service.service

import no.fdk.fdk_reasoning_service.model.CatalogType
import no.fdk.fdk_reasoning_service.model.TurtleDBO
import no.fdk.fdk_reasoning_service.rdf.CPSV
import no.fdk.fdk_reasoning_service.rdf.CV
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.RDFNode
import org.apache.jena.rdf.model.Resource
import org.apache.jena.riot.Lang
import org.apache.jena.vocabulary.DCTerms
import org.apache.jena.vocabulary.RDF
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.findById
import org.springframework.stereotype.Service

private val LOGGER = LoggerFactory.getLogger(EventService::class.java)

@Service
class PublicServiceService(
    private val reasoningService: ReasoningService,
    private val publicServiceMongoTemplate: MongoTemplate
) {

    fun reasonHarvestedPublicServices() {
        publicServiceMongoTemplate.findById<TurtleDBO>("services-union-graph", "turtle")
            ?.let { parseRDFResponse(ungzip(it.turtle), Lang.TURTLE, "public-services") }
            ?.let { reasoningService.catalogReasoning(it, CatalogType.PUBLICSERVICES) }
            ?. run { separateAndSavePublicServices() }
            ?: run { LOGGER.error("harvested public services not found", Exception()) }
    }

    private fun Model.separateAndSavePublicServices() {
        publicServiceMongoTemplate.save(createUnionDBO(), "fdkPublicService")

        splitPublicServicesFromRDF()
            .forEach { publicServiceMongoTemplate.save(it.second.createDBO(it.first), "fdkPublicService") }
        LOGGER.debug("reasoned public services saved to db")
    }

    private fun Model.splitPublicServicesFromRDF(): List<Pair<String, Model>> =
        listResourcesWithProperty(RDF.type, CPSV.PublicService)
            .toList()
            .mapNotNull { it.extractPublicServiceModel(nsPrefixMap) }

    private fun Resource.extractPublicServiceModel(nsPrefixes: Map<String, String>): Pair<String, Model>? {
        var serviceModel = listProperties().toModel()
        serviceModel.setNsPrefixes(nsPrefixes)

        listProperties().toList()
            .filter { it.isResourceProperty() }
            .forEach {
                serviceModel = serviceModel.recursiveAddNonPublicServiceResources(it.resource, 10)
            }

        val recordURI = catalogRecordURI()
        if (recordURI == null) {
            LOGGER.error("Unable to find record for $uri", Exception())
            return null
        }

        serviceModel = serviceModel.union(catalogRecordModel(recordURI))

        val fdkId = fdkId(recordURI)
        if (fdkId == null) {
            LOGGER.error("Unable to find fdkId for $recordURI", Exception())
            return null
        }

        return Pair(fdkId, serviceModel)
    }

    private fun Resource.catalogRecordModel(recordURI: String): Model =
        ModelFactory.createDefaultModel()
            .recursiveAddNonPublicServiceResources(model.getResource(recordURI), 4)

    private fun Model.recursiveAddNonPublicServiceResources(resource: Resource, recursiveCount: Int): Model {
        val newCount = recursiveCount - 1
        val types = resource.listProperties(RDF.type)
            .toList()
            .map { it.`object` }

        if (resourceShouldBeAdded(resource.uri, types)) {
            add(resource.listProperties())

            if (newCount > 0) {
                resource.listProperties().toList()
                    .filter { it.isResourceProperty() }
                    .forEach { recursiveAddNonPublicServiceResources(it.resource, newCount) }
            }
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

    private fun Model.resourceShouldBeAdded(resourceURI: String?, types: List<RDFNode>): Boolean =
        when {
            types.contains(CPSV.PublicService) -> false
            types.contains(CV.BusinessEvent) -> false
            types.contains(CV.LifeEvent) -> false
            resourceURI == null -> true
            containsTriple("<${resourceURI}>", "a", "?o") -> false
            else -> true
        }

}
