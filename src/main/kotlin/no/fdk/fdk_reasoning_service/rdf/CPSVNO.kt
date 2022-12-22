package no.fdk.fdk_reasoning_service.rdf

import org.apache.jena.rdf.model.Property
import org.apache.jena.rdf.model.ResourceFactory

class CPSVNO {

    companion object {
        const val uri = "https://data.norge.no/vocabulary/cpsvno#"
        val Service: Property = ResourceFactory.createProperty("${uri}Service")
    }

}
