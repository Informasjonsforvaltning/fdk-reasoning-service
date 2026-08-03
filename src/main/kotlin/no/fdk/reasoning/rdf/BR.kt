package no.fdk.reasoning.rdf

import org.apache.jena.rdf.model.Property
import org.apache.jena.rdf.model.ResourceFactory

class BR {
    companion object {
        const val URI =
            "https://raw.githubusercontent.com/Informasjonsforvaltning/organization-catalog/main/src/main/resources/ontology/organization-catalog.owl#"

        val orgPath: Property = ResourceFactory.createProperty("${URI}orgPath")
    }
}
