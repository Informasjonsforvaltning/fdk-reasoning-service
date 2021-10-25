package no.fdk.fdk_reasoning_service.service

import no.fdk.fdk_reasoning_service.config.ApplicationURI
import no.fdk.fdk_reasoning_service.model.CatalogType
import org.apache.jena.rdf.model.Model

const val RECORDS_PARAM_TRUE = "catalogrecords=true"

fun Model.fdkPrefix(): Model =
    setNsPrefix("fdk", "https://raw.githubusercontent.com/Informasjonsforvaltning/fdk-reasoning-service/master/src/main/resources/ontology/fdk.owl#")

fun CatalogType.uri(uris: ApplicationURI): String =
    when (this) {
        CatalogType.DATASETS -> "${uris.datasets}?$RECORDS_PARAM_TRUE"
    }
