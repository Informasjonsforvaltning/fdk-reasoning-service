package no.fdk.fdk_reasoning_service.service

import no.fdk.fdk_reasoning_service.config.ApplicationURI
import no.fdk.fdk_reasoning_service.model.CatalogType
import no.fdk.fdk_reasoning_service.rdf.BR
import org.apache.jena.rdf.model.*
import org.apache.jena.sparql.vocabulary.FOAF
import org.apache.jena.vocabulary.DCTerms
import org.apache.jena.vocabulary.RDF
import org.apache.jena.vocabulary.ROV
import org.apache.jena.vocabulary.SKOS

const val RECORDS_PARAM_TRUE = "catalogrecords=true"

fun Model.fdkPrefix(): Model =
    setNsPrefix("fdk", "https://raw.githubusercontent.com/Informasjonsforvaltning/fdk-reasoning-service/master/src/main/resources/ontology/fdk.owl#")

fun CatalogType.uri(uris: ApplicationURI): String =
    when (this) {
        CatalogType.DATASETS -> "${uris.datasets}?$RECORDS_PARAM_TRUE"
        CatalogType.DATASERVICES -> "${uris.dataservices}?$RECORDS_PARAM_TRUE"
    }

fun Model.createModelOfPublishersWithOrgData(publisherURIs: Set<String>, orgsURI: String): Model {
    val model = ModelFactory.createDefaultModel()
    model.setNsPrefixes(nsPrefixMap)

    publisherURIs.map { Pair(it, orgResourceForPublisher(it, orgsURI)) }
        .filter { it.second != null }
        .forEach {
            model.createResource(it.first).addPropertiesFromOrgResource(it.second)
        }

    return model
}

fun Model.orgResourceForPublisher(publisherURI: String, orgsURI: String): Resource? =
    orgIdFromURI(publisherURI)
        ?.let { getResource("$orgsURI/$it") }

fun Resource.addPropertiesFromOrgResource(orgResource: Resource?) {
    if (orgResource != null) {
        safeAddProperty(RDF.type, orgResource.getProperty(RDF.type)?.`object`)
        safeAddProperty(DCTerms.identifier, orgResource.getProperty(DCTerms.identifier)?.`object`)
        safeAddProperty(BR.orgPath, orgResource.getProperty(BR.orgPath)?.`object`)
        safeAddProperty(ROV.legalName, orgResource.getProperty(ROV.legalName)?.`object`)
        safeAddProperty(FOAF.name, orgResource.getProperty(FOAF.name)?.`object`)
        addOrgType(orgResource)
    }
}

fun Resource.safeAddProperty(property: Property, value: RDFNode?): Resource =
    if (value == null) this
    else addProperty(property, value)

private fun Resource.addOrgType(orgResource: Resource): Resource {
    val orgType = orgResource.getProperty(ROV.orgType)
    if (orgType != null && orgType.isResourceProperty()) {
        orgType.resource
            .getProperty(SKOS.prefLabel)
            ?.`object`
            ?.let {
                addProperty(
                    ROV.orgType,
                    model.createResource(SKOS.Concept)
                        .addProperty(SKOS.prefLabel, it))
            }
    }

    return this
}

fun Model.extractInadequatePublishers(): Set<String> =
    listResourcesWithProperty(DCTerms.publisher)
        .toList()
        .flatMap { it.listProperties(DCTerms.publisher).toList() }
        .asSequence()
        .filter { it.isResourceProperty() }
        .map { it.resource }
        .filter {  it.isURIResource }
        .filter { it.dctIdentifierIsInadequate() }
        .mapNotNull { it.uri }
        .toSet()

fun Statement.isResourceProperty(): Boolean =
    try {
        resource.isResource
    } catch (ex: ResourceRequiredException) {
        false
    }

fun Resource.dctIdentifierIsInadequate(): Boolean =
    listProperties(DCTerms.identifier)
        .toList()
        .map { it.`object` }
        .mapNotNull { it.extractPublisherId() }
        .isEmpty()

fun RDFNode.extractPublisherId(): String? =
    when {
        isURIResource -> orgIdFromURI(asResource().uri)
        isLiteral -> orgIdFromURI(asLiteral().string)
        else -> null
    }

fun orgIdFromURI(uri: String): String? {
    val regex = Regex("""[0-9]{9}""")
    val allMatching = regex.findAll(uri).toList()

    return if (allMatching.size == 1) allMatching.first().value
    else null
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
    "data.norge.no/nlod")
