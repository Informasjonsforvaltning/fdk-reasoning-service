package no.fdk.fdk_reasoning_service.rdf

import org.apache.jena.rdf.model.Property
import org.apache.jena.rdf.model.Resource
import org.apache.jena.rdf.model.ResourceFactory

class DCATNO {
    companion object {
        const val uri = "https://data.norge.no/vocabulary/dcatno#"
        val containsService: Property = ResourceFactory.createProperty( "${uri}containsService")
    }
}
