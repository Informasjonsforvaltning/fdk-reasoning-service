package no.fdk.fdk_reasoning_service.model

import org.apache.jena.rdf.model.Model

data class ExternalRDFData(
    val orgData: Model,
    val losData: Model
)
