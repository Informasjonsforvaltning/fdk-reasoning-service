package no.fdk.fdk_reasoning_service.service

import no.fdk.fdk_reasoning_service.model.CatalogType
import org.apache.jena.query.QueryExecutionFactory
import org.apache.jena.query.QueryFactory
import org.apache.jena.rdf.model.*
import org.apache.jena.riot.Lang
import java.io.ByteArrayOutputStream
import java.io.StringReader

fun Model.createRDFResponse(responseType: Lang): String =
    ByteArrayOutputStream().use { out ->
        write(out, responseType.name)
        out.flush()
        out.toString("UTF-8")
    }

fun parseRDFResponse(responseBody: String, rdfLanguage: Lang): Model {
    val responseModel = ModelFactory.createDefaultModel()
    responseModel.read(StringReader(responseBody), "", rdfLanguage.name)
    return responseModel
}

fun Model.fdkPrefix(): Model =
    setNsPrefix("fdk", "https://raw.githubusercontent.com/Informasjonsforvaltning/fdk-reasoning-service/main/src/main/resources/ontology/fdk.owl#")

fun Resource.safeAddProperty(property: Property, value: RDFNode?): Resource =
    if (value == null) this
    else addProperty(property, value)

fun Statement.isResourceProperty(): Boolean =
    try {
        resource.isResource
    } catch (ex: ResourceRequiredException) {
        false
    }

fun Model.containsTriple(subj: String, pred: String, obj: String): Boolean {
    val askQuery = "ASK { $subj $pred $obj }"

    return try {
        val query = QueryFactory.create(askQuery)
        return QueryExecutionFactory.create(query, this).execAsk()
    } catch (ex: Exception) { false }
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

val openDataURIBases: Set<String> = setOf(
    "creativecommons.org/licenses/by/4.0/deed.no",
    "data.norge.no/nlod/no/1.0",
    "creativecommons.org/publicdomain/zero/1.0",
    "data.norge.no/nlod/no/2.0",
    "creativecommons.org/licenses/by/4.0",
    "data.norge.no/nlod/no",
    "data.norge.no/nlod",
    "publications.europa.eu/resource/authority/licence/CC0",
    "publications.europa.eu/resource/authority/licence/NLOD_2_0",
    "publications.europa.eu/resource/authority/licence/CC_BY_4_0")

fun modelOfContainedReferenceData(
    inputModel: Model,
    referenceDataModel: Model,
): Model {
    val m = ModelFactory.createDefaultModel()

    referenceDataModel.listSubjects()
        .toList()
        .filter { inputModel.containsTriple("?s", "?p", "<${it.uri}>") }
        .forEach { it.recursiveAddReferenceCodeProperties(m) }

    return m
}

private fun Resource.recursiveAddReferenceCodeProperties(m: Model) {
    listProperties()
        .toList()
        .filter { !m.contains(it) }
        .also { m.add(it) }
        .filter { it.isResourceProperty() }
        .map { it.resource }
        .filter { it.isAnon }
        .forEach { it.recursiveAddReferenceCodeProperties(m) }
}
