package no.fdk.reasoning.rdf

import org.apache.jena.rdf.model.Property
import org.apache.jena.rdf.model.ResourceFactory

class PROV {
    companion object {
        const val URI = "http://www.w3.org/ns/prov#"

        val agent: Property = ResourceFactory.createProperty("${URI}agent")
        val qualifiedAttribution: Property = ResourceFactory.createProperty("${URI}qualifiedAttribution")
    }
}
