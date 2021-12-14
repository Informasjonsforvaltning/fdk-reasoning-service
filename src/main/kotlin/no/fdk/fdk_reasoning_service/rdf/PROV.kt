package no.fdk.fdk_reasoning_service.rdf

import org.apache.jena.rdf.model.Property
import org.apache.jena.rdf.model.ResourceFactory

class PROV {

    companion object {
        const val uri = "http://www.w3.org/ns/prov#"

        val agent: Property = ResourceFactory.createProperty("${uri}agent")
        val qualifiedAttribution: Property = ResourceFactory.createProperty("${uri}qualifiedAttribution")
    }

}
