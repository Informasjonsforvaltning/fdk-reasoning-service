package no.fdk.fdk_reasoning_service.service

import no.fdk.fdk_reasoning_service.model.CatalogType
import no.fdk.fdk_reasoning_service.model.TurtleDBO
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.Resource
import org.apache.jena.riot.Lang
import org.apache.jena.vocabulary.RDF
import org.apache.jena.vocabulary.SKOS
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.findById
import org.springframework.stereotype.Service

private val LOGGER = LoggerFactory.getLogger(ConceptService::class.java)

@Service
class ConceptService(
    private val reasoningService: ReasoningService,
    private val conceptMongoTemplate: MongoTemplate
) {

    fun reasonHarvestedConcepts() {
        conceptMongoTemplate.findById<TurtleDBO>("collection-union-graph", "turtle")
            ?.let { parseRDFResponse(ungzip(it.turtle), Lang.TURTLE, "concepts") }
            ?.let { reasoningService.catalogReasoning(it, CatalogType.CONCEPTS) }
            ?. run { separateAndSaveConcepts() }
            ?: run { LOGGER.error("harvested concepts not found", Exception()) }
    }

    private fun Model.separateAndSaveConcepts() {
        conceptMongoTemplate.save(createUnionDBO(), "fdkCollections")

        splitConceptCollectionsFromRDF()
            .forEach { it.saveCollectionAndConceptModels() }
        LOGGER.debug("reasoned concepts saved to db")
    }

    private fun CollectionAndConcepts.saveCollectionAndConceptModels() {
        conceptMongoTemplate.save(collection.createDBO(fdkId), "fdkCollections")

        concepts.forEach { conceptMongoTemplate.save(it.concept.createDBO(it.fdkId), "fdkConcepts") }
    }

    private fun Model.splitConceptCollectionsFromRDF(): List<CollectionAndConcepts> =
        listResourcesWithProperty(RDF.type, SKOS.Collection)
            .toList()
            .mapNotNull { collectionResource -> collectionResource.extractCollection() }

    private fun Resource.extractCollection(): CollectionAndConcepts? {
        val fdkIdAndRecordURI = extractFDKIdAndRecordURI()
        return if (fdkIdAndRecordURI == null) null
        else {
            val collectionConcepts: List<ConceptModel> = listProperties(SKOS.member)
                .toList()
                .mapNotNull { concept -> concept.resource.extractConcept() }

            var collectionModelWithoutConcepts = listProperties().toModel()
            collectionModelWithoutConcepts.setNsPrefixes(model.nsPrefixMap)

            listProperties().toList()
                .filter { it.isResourceProperty() }
                .forEach {
                    if (it.predicate != SKOS.member) {
                        collectionModelWithoutConcepts =
                            collectionModelWithoutConcepts.recursiveAddNonConceptResource(it.resource, 5)
                    }
                }

            var conceptsUnion = ModelFactory.createDefaultModel()
            collectionConcepts.forEach { conceptsUnion = conceptsUnion.union(it.concept) }

            CollectionAndConcepts(
                fdkId = fdkIdAndRecordURI.fdkId,
                concepts = collectionConcepts,
                collection = collectionModelWithoutConcepts
                    .union(conceptsUnion)
                    .union(catalogRecordModel(fdkIdAndRecordURI.recordURI))
            )
        }
    }

    private fun Resource.extractConcept(): ConceptModel? {
        var conceptModel = listProperties().toModel()
        conceptModel = conceptModel.setNsPrefixes(model.nsPrefixMap)

        listProperties().toList()
            .filter { it.isResourceProperty() }
            .forEach { conceptModel = conceptModel.recursiveAddNonConceptResource(it.resource, 10) }

        val fdkIdAndRecordURI = extractFDKIdAndRecordURI()

        return if (fdkIdAndRecordURI == null) null
        else ConceptModel(
            fdkId = fdkIdAndRecordURI.fdkId,
            concept = conceptModel.union(catalogRecordModel(fdkIdAndRecordURI.recordURI))
        )
    }

    private fun Model.recursiveAddNonConceptResource(resource: Resource, maxDepth: Int): Model {
        val newDepth = maxDepth - 1

        if (resourceShouldBeAdded(resource)) {
            add(resource.listProperties())

            if (newDepth > 0) {
                resource.listProperties().toList()
                    .filter { it.isResourceProperty() }
                    .forEach { recursiveAddNonConceptResource(it.resource, newDepth) }
            }
        }

        return this
    }

    private fun Model.resourceShouldBeAdded(resource: Resource): Boolean {
        val types = resource.listProperties(RDF.type)
            .toList()
            .map { it.`object` }

        return when {
            types.contains(SKOS.Concept) -> false
            containsTriple("<${resource.uri}>", "a", "?o") -> false
            else -> true
        }
    }

    private data class CollectionAndConcepts(
        val fdkId: String,
        val collection: Model,
        val concepts: List<ConceptModel>
    )

    private data class ConceptModel(
        val fdkId: String,
        val concept: Model
    )

}
