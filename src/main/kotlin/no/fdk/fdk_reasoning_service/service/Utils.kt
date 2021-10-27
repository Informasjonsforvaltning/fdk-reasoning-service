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

val napThemes: Set<String> = setOf(
    "https://psi.norge.no/los/tema/mobilitetstilbud",
    "https://psi.norge.no/los/tema/trafikkinformasjon",
    "https://psi.norge.no/los/tema/veg-og-vegregulering",
    "https://psi.norge.no/los/tema/yrkestransport",
    "https://psi.norge.no/los/ord/ruteinformasjon",
    "https://psi.norge.no/los/ord/lokasjonstjenester",
    "https://psi.norge.no/los/ord/tilrettelagt-transport",
    "https://psi.norge.no/los/ord/miljovennlig-transport",
    "https://psi.norge.no/los/ord/takster-og-kjopsinformasjon",
    "https://psi.norge.no/los/ord/reisegaranti",
    "https://psi.norge.no/los/ord/reisebillett",
    "https://psi.norge.no/los/ord/parkering-og-hvileplasser",
    "https://psi.norge.no/los/ord/drivstoff-og-ladestasjoner",
    "https://psi.norge.no/los/ord/skoleskyss",
    "https://psi.norge.no/los/ord/ruteplanlegger",
    "https://psi.norge.no/los/ord/veg--og-foreforhold",
    "https://psi.norge.no/los/ord/sanntids-trafikkinformasjon",
    "https://psi.norge.no/los/ord/bominformasjon",
    "https://psi.norge.no/los/ord/trafikksignaler-og-reguleringer",
    "https://psi.norge.no/los/ord/vegarbeid",
    "https://psi.norge.no/los/ord/trafikksikkerhet",
    "https://psi.norge.no/los/ord/persontransport",
    "https://psi.norge.no/los/ord/godstransport",
    "https://psi.norge.no/los/ord/feiing-og-stroing",
    "https://psi.norge.no/los/ord/aksellastrestriksjoner",
    "https://psi.norge.no/los/ord/broyting",
    "https://psi.norge.no/los/ord/gangveg",
    "https://psi.norge.no/los/ord/vegnett",
    "https://psi.norge.no/los/ord/gatelys",
    "https://psi.norge.no/los/ord/vegbygging",
    "https://psi.norge.no/los/ord/privat-vei",
    "https://psi.norge.no/los/ord/vegvedlikehold",
    "https://psi.norge.no/los/ord/gravemelding",
    "https://psi.norge.no/los/ord/sykkel")
