package no.fdk.fdk_reasoning_service.service

import no.fdk.fdk_reasoning_service.model.CatalogType
import no.fdk.fdk_reasoning_service.model.ExternalRDFData
import no.fdk.fdk_reasoning_service.model.HarvestReport
import no.fdk.fdk_reasoning_service.model.ReasoningReport
import no.fdk.fdk_reasoning_service.model.TurtleDBO
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.RDFNode
import org.apache.jena.rdf.model.Resource
import org.apache.jena.rdf.model.Statement
import org.apache.jena.riot.Lang
import org.apache.jena.vocabulary.DCTerms
import org.apache.jena.vocabulary.RDF
import org.apache.jena.vocabulary.SKOS
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.findAll
import org.springframework.data.mongodb.core.findById
import org.springframework.stereotype.Service
import java.util.Date

private val LOGGER = LoggerFactory.getLogger(ConceptService::class.java)

@Service
class ConceptService(
    private val reasoningService: ReasoningService,
    private val conceptMongoTemplate: MongoTemplate
) {

    fun getAllConceptCollections(lang: Lang): String =
        conceptMongoTemplate.findById<TurtleDBO>(UNION_ID, "fdkCollections")
            ?.toRDF(lang)
            ?: ModelFactory.createDefaultModel().createRDFResponse(lang)

    fun getConceptCollectionById(id: String, lang: Lang): String? =
        conceptMongoTemplate.findById<TurtleDBO>(id, "fdkCollections")
            ?.toRDF(lang)

    fun getConceptById(id: String, lang: Lang): String? =
        conceptMongoTemplate.findById<TurtleDBO>(id, "fdkConcepts")
            ?.toRDF(lang)

    fun reasonReportedChanges(harvestReport: HarvestReport, rdfData: ExternalRDFData, start: Date): ReasoningReport =
        try {
            harvestReport.changedCatalogs
                .forEach { reasonCollectionConcepts(it.fdkId, rdfData) }

            ReasoningReport(
                id = harvestReport.id,
                url = harvestReport.url,
                dataType = CatalogType.CONCEPTS.toReportType(),
                harvestError = false,
                startTime = start.formatWithOsloTimeZone(),
                endTime = formatNowWithOsloTimeZone(),
                changedCatalogs = harvestReport.changedCatalogs,
                changedResources = harvestReport.changedResources,
                removedResources = harvestReport.removedResources
            )
        } catch (ex: Exception) {
            LOGGER.error("concept reasoning failed for ${harvestReport.url}", ex)
            ReasoningReport(
                id = harvestReport.id,
                url = harvestReport.url,
                dataType = CatalogType.CONCEPTS.toReportType(),
                harvestError = true,
                errorMessage = ex.message,
                startTime = start.formatWithOsloTimeZone(),
                endTime = formatNowWithOsloTimeZone()
            )
        }

    fun updateUnion() {
        var collectionUnion = ModelFactory.createDefaultModel()

        conceptMongoTemplate.findAll<TurtleDBO>("fdkCollections")
            .filter { it.id != UNION_ID }
            .map { parseRDFResponse(ungzip(it.turtle), Lang.TURTLE, null) }
            .forEach { collectionUnion = collectionUnion.union(it) }

        conceptMongoTemplate.save(collectionUnion.createUnionDBO(), "fdkCollections")
    }

    private fun reasonCollectionConcepts(collectionId: String, rdfData: ExternalRDFData) {
        conceptMongoTemplate.findById<TurtleDBO>(harvestedCollectionID(collectionId), "turtle")
            ?.let { parseRDFResponse(ungzip(it.turtle), Lang.TURTLE, "concepts") }
            ?.let { reasoningService.catalogReasoning(it, CatalogType.CONCEPTS, rdfData) }
            ?.union(rdfData.conceptStatuses)
            ?.union(rdfData.conceptSubjects)
            ?.also { it.separateAndSaveConcepts() }
            ?: throw Exception("missing database data, harvest-reasoning was stopped")
    }

    private fun Model.separateAndSaveConcepts() {
        splitConceptCollectionsFromRDF()
            .forEach { it.saveCollectionAndConceptModels() }
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
                .filter { it.isResourceProperty() }
                .filter { it.resource.hasProperty(RDF.type, SKOS.Concept) }
                .mapNotNull { concept -> concept.resource.extractConcept() }

            var collectionModelWithoutConcepts = listProperties().toModel()
            collectionModelWithoutConcepts.setNsPrefixes(model.nsPrefixMap)

            listProperties().toList()
                .filter { it.isResourceProperty() }
                .forEach {
                    if (it.predicate != SKOS.member) {
                        collectionModelWithoutConcepts =
                            collectionModelWithoutConcepts.recursiveAddNonConceptResource(it.resource, this)
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
            .forEach { conceptModel = conceptModel.recursiveAddNonConceptResource(it.resource, this ) }

        val fdkIdAndRecordURI = extractFDKIdAndRecordURI()

        return if (fdkIdAndRecordURI == null) null
        else ConceptModel(
            fdkId = fdkIdAndRecordURI.fdkId,
            concept = conceptModel.union(catalogRecordModel(fdkIdAndRecordURI.recordURI))
        )
    }

    private fun Model.recursiveAddNonConceptResource(resource: Resource, current: Resource): Model {
        if (resourceShouldBeAdded(resource, current)) {
            add(resource.listProperties())

            resource.listProperties().toList()
                .filter { it.isResourceProperty() }
                .forEach { recursiveAddNonConceptResource(it.resource, current) }
        }

        return this
    }

    private fun Model.resourceShouldBeAdded(resource: Resource, current: Resource): Boolean {
        val types = resource.listProperties(RDF.type)
            .toList()
            .map { it.`object` }

        return when {
            types.contains(SKOS.Concept) && !current.hasProperty(DCTerms.subject, resource) -> false
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

    private fun harvestedCollectionID(fdkId: String): String =
        "collection-$fdkId"

}
