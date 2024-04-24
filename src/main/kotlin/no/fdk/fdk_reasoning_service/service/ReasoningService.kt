package no.fdk.fdk_reasoning_service.service

import no.fdk.fdk_reasoning_service.model.CatalogType
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.riot.Lang
import org.springframework.stereotype.Service

@Service
class ReasoningService() {
    fun reasonGraph(graph: String, catalogType: CatalogType): String {
        val inputModel = parseRDFResponse(graph, Lang.TURTLE)

        return ModelFactory.createDefaultModel()
            .add(inputModel)
            .createRDFResponse(Lang.TURTLE)
    }

}
