package no.fdk.reasoning.service

import no.fdk.reasoning.model.CatalogType
import org.apache.jena.rdf.model.Model

sealed interface Reasoner {
    fun reason(
        inputModel: Model,
        catalogType: CatalogType,
    ): Model
}
