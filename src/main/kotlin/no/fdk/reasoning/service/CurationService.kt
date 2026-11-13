package no.fdk.reasoning.service

import no.fdk.reasoning.cache.CurationCache
import no.fdk.reasoning.cache.HIGH_VALUE_DATASET_TYPE_NAMESPACE
import no.fdk.reasoning.model.CatalogType
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.RDFNode
import org.apache.jena.sparql.vocabulary.FOAF
import org.apache.jena.vocabulary.DCAT
import org.apache.jena.vocabulary.DCTerms
import org.apache.jena.vocabulary.RDF
import org.springframework.stereotype.Service

@Service
class CurationService(private val curationCache: CurationCache) {
    fun curate(model: Model, catalogType: CatalogType) {
        if (catalogType != CatalogType.DATASETS) return
        if (!curationCache.isLoaded()) throw Exception("Curated dataset types are missing in curation cache")

        model
            .listResourcesWithProperty(RDF.type, DCAT.CatalogRecord)
            .toList()
            .forEach { record ->
                val fdkId = record.getProperty(DCTerms.identifier)?.string
                val dataset = record.getPropertyResourceValue(FOAF.primaryTopic)
                if (fdkId != null && dataset != null && model.contains(dataset, RDF.type, DCAT.Dataset)) {
                    model
                        .listStatements(dataset, DCTerms.type, null as RDFNode?)
                        .toList()
                        .filter { it.`object`.isURIResource && it.resource.uri.startsWith(HIGH_VALUE_DATASET_TYPE_NAMESPACE) }
                        .forEach { model.remove(it) }

                    curationCache
                        .datasetTypes(fdkId)
                        .forEach { dataset.addProperty(DCTerms.type, model.createResource(it)) }
                }
            }
    }
}
