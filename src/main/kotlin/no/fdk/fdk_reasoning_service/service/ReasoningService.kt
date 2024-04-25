package no.fdk.fdk_reasoning_service.service

import no.fdk.fdk_reasoning_service.model.CatalogType
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.Resource
import org.apache.jena.riot.Lang
import org.apache.jena.vocabulary.DCAT
import org.apache.jena.vocabulary.DCTerms
import org.apache.jena.vocabulary.RDF
import org.springframework.stereotype.Service

@Service
class ReasoningService(
    private val organizationService: OrganizationService,
    private val referenceDataService: ReferenceDataService,
    private val deductionService: DeductionService
) {
    fun CatalogType.reason(graph: String, fdkId: String): String {
        val inputModel = parseRDFResponse(graph, Lang.TURTLE)

        return deductionService.deductionsModel(inputModel, this)
            .add(organizationService.extraOrganizationTriples(inputModel, this))
            .add(referenceDataService.referenceDataModel(this))
            .add(inputModel)
            .extractReasonedModel(fdkId)
            .createRDFResponse(Lang.TURTLE)
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
