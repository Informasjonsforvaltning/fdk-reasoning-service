package no.fdk.fdk_reasoning_service.rdf

import org.apache.jena.rdf.model.Property
import org.apache.jena.rdf.model.ResourceFactory

class CV {

    companion object {
        const val uri = "http://data.europa.eu/m8g/"

        val hasCompetentAuthority: Property = ResourceFactory.createProperty("${uri}hasCompetentAuthority")
        val BusinessEvent: Property = ResourceFactory.createProperty("${uri}BusinessEvent")
        val LifeEvent: Property = ResourceFactory.createProperty("${uri}LifeEvent")
        val Participation: Property = ResourceFactory.createProperty("${uri}Participation")
        val playsRole: Property = ResourceFactory.createProperty("${uri}playsRole")
    }

}
