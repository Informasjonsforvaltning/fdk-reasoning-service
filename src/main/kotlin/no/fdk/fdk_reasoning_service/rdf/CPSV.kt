package no.fdk.fdk_reasoning_service.rdf

import org.apache.jena.rdf.model.Property
import org.apache.jena.rdf.model.ResourceFactory

class CPSV {

    companion object {
        const val uri = "http://purl.org/vocab/cpsv#"

        val PublicService: Property = ResourceFactory.createProperty("${uri}PublicService")
    }

}
