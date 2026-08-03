package no.fdk.reasoning.rdf

import org.apache.jena.rdf.model.Property
import org.apache.jena.rdf.model.ResourceFactory

class FDK {
    companion object {
        const val URI =
            "https://fellesdatakatalog.digdir.no/ontology/internal/"

        val themePath: Property = ResourceFactory.createProperty("${URI}themePath")
    }
}
