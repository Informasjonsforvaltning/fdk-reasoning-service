package no.fdk.fdk_reasoning_service.service

import no.fdk.fdk_reasoning_service.model.CatalogType
import org.apache.jena.rdf.model.Model

sealed interface Reasoner {
    fun reason(inputModel: Model, catalogType: CatalogType): Model
}
