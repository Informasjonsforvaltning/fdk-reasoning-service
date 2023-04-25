package no.fdk.fdk_reasoning_service.rdf

import org.apache.jena.rdf.model.Property
import org.apache.jena.rdf.model.ResourceFactory

class FDK {

    companion object {
        const val uri =
            "https://fellesdatakatalog.digdir.no/ontology/internal/"

        val themePath: Property = ResourceFactory.createProperty("${uri}themePath")
    }

}
