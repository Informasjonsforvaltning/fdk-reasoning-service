package no.fdk.reasoning.rdf

import org.apache.jena.rdf.model.Property
import org.apache.jena.rdf.model.ResourceFactory

class CV {
    companion object {
        const val URI = "http://data.europa.eu/m8g/"

        val hasCompetentAuthority: Property = ResourceFactory.createProperty("${URI}hasCompetentAuthority")
        val ownedBy: Property = ResourceFactory.createProperty("${URI}ownedBy")
        val thematicArea: Property = ResourceFactory.createProperty("${URI}thematicArea")
    }
}
