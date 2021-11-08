package no.fdk.fdk_reasoning_service.rdf

import org.apache.jena.rdf.model.Property
import org.apache.jena.rdf.model.ResourceFactory

class ModellDCATAPNO {
    companion object {
        const val uri = "https://data.norge.no/vocabulary/modelldcatno#"
        val InformationModel: Property = ResourceFactory.createProperty( "${uri}InformationModel")
        val CodeList: Property = ResourceFactory.createProperty("${uri}CodeList")
        val CodeElement: Property = ResourceFactory.createProperty("${uri}CodeElement")
        val model: Property = ResourceFactory.createProperty( "${uri}model")
    }
}
