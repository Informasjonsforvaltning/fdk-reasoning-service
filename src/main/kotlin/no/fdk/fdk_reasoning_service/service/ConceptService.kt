package no.fdk.fdk_reasoning_service.service

import no.fdk.fdk_reasoning_service.model.CatalogType
import no.fdk.fdk_reasoning_service.model.ExternalRDFData
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.Resource
import org.apache.jena.riot.Lang
import org.apache.jena.vocabulary.DCAT
import org.apache.jena.vocabulary.DCTerms
import org.apache.jena.vocabulary.RDF
import org.springframework.stereotype.Service

@Service
class ConceptService(
    private val reasoningService: ReasoningService
) {
    fun reasonConcept(graph: String, fdkId: String, rdfData: ExternalRDFData) : String =
        parseRDFResponse(graph, Lang.TURTLE, "concepts")
            ?.let { reasoningService.catalogReasoning(it, CatalogType.CONCEPTS, rdfData) }
            ?.union(rdfData.toModel())
            ?.extractReasonedModel(fdkId)
            ?.createRDFResponse(Lang.TURTLE)
            ?: throw Exception("Reasoning of concept failed")

    private fun ExternalRDFData.toModel(): Model {
        val m = ModelFactory.createDefaultModel()
        m.add(conceptStatuses)
        m.add(conceptSubjects)
        return m
    }

    private fun Model.extractReasonedModel(fdkId: String): Model {
        val conceptRecord = listSubjectsWithProperty(DCTerms.identifier, fdkId)
            .toList()
            .first { it.hasProperty(RDF.type, DCAT.CatalogRecord) }
        val model = conceptRecord
            .listProperties()
            .toModel()
        model.setNsPrefixes(nsPrefixMap)

        conceptRecord
            .listProperties()
            .toList()
            .filter { it.isResourceProperty() }
            .forEach { model.recursiveAddResources(it.resource) }

        return model
    }

    private fun Model.recursiveAddResources(resource: Resource): Model {
        if (!containsTriple("<${resource.uri}>", "a", "?o")) {
            add(resource.listProperties())

            resource.listProperties().toList()
                .filter { it.isResourceProperty() }
                .forEach { recursiveAddResources(it.resource) }
        }

        return this
    }

}
