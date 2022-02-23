package no.fdk.fdk_reasoning_service.rdf

import org.apache.jena.rdf.model.Property
import org.apache.jena.rdf.model.ResourceFactory

class BR {

    companion object {
        const val uri =
            "https://raw.githubusercontent.com/Informasjonsforvaltning/organization-catalog/master/src/main/resources/ontology/organization-catalog.owl#"

        val orgPath: Property = ResourceFactory.createProperty("${uri}orgPath")
    }

}
