package no.fdk.fdk_reasoning_service.rdf

import org.apache.jena.rdf.model.Property
import org.apache.jena.rdf.model.Resource
import org.apache.jena.rdf.model.ResourceFactory

class CV {

    companion object {
        const val uri = "http://data.europa.eu/m8g/"

        val hasCompetentAuthority: Property = ResourceFactory.createProperty("${uri}hasCompetentAuthority")
        val ownedBy: Property = ResourceFactory.createProperty("${uri}ownedBy")
        val playsRole: Property = ResourceFactory.createProperty("${uri}playsRole")
        val Event: Resource = ResourceFactory.createResource("${uri}Event")
        val BusinessEvent: Resource = ResourceFactory.createResource("${uri}BusinessEvent")
        val LifeEvent: Resource = ResourceFactory.createResource("${uri}LifeEvent")
        val Participation: Resource = ResourceFactory.createResource("${uri}Participation")
    }

}
