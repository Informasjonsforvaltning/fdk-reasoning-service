package no.fdk.fdk_reasoning_service.service

import no.fdk.fdk_reasoning_service.Application
import no.fdk.fdk_reasoning_service.rdf.BR
import no.fdk.fdk_reasoning_service.rdf.PROV
import org.apache.jena.query.QueryExecutionFactory
import org.apache.jena.query.QueryFactory
import org.apache.jena.rdf.model.*
import org.apache.jena.riot.Lang
import org.apache.jena.riot.RDFDataMgr
import org.apache.jena.sparql.vocabulary.FOAF
import org.apache.jena.vocabulary.DCTerms
import org.apache.jena.vocabulary.RDF
import org.apache.jena.vocabulary.ROV
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.io.StringReader
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

private val logger = LoggerFactory.getLogger(Application::class.java)

private const val dateFormat: String = "yyyy-MM-dd HH:mm:ss Z"

fun Model.createRDFResponse(responseType: Lang): String =
    ByteArrayOutputStream().use { out ->
        write(out, responseType.name)
        out.flush()
        out.toString("UTF-8")
    }

fun parseRDFResponse(responseBody: String, rdfLanguage: Lang, rdfSource: String?): Model? {
    val responseModel = ModelFactory.createDefaultModel()

    try {
        responseModel.read(StringReader(responseBody), "", rdfLanguage.name)
    } catch (ex: Exception) {
        logger.error("Parse from $rdfSource has failed", ex)
        return null
    }

    return responseModel
}

fun Model.fdkPrefix(): Model =
    setNsPrefix("fdk", "https://raw.githubusercontent.com/Informasjonsforvaltning/fdk-reasoning-service/main/src/main/resources/ontology/fdk.owl#")

fun Model.createModelOfOrganizationsWithOrgData(organizationURIs: Set<String>, orgsURI: String): Model {
    val model = ModelFactory.createDefaultModel()
    model.setNsPrefixes(nsPrefixMap)

    organizationURIs.map { Pair(it, orgResourceForOrganization(it, orgsURI)) }
        .filter { it.second != null }
        .forEach {
            model.createResource(it.first).addPropertiesFromOrgResource(it.second)
        }

    return model
}

fun Model.orgResourceForOrganization(organizationURI: String, orgsURI: String): Resource? =
    orgIdFromURI(organizationURI)
        ?.let { downloadOrgDataIfMissing("$orgsURI/${orgIdFromURI(organizationURI)}") }

fun Model.downloadOrgDataIfMissing(uri: String): Resource? =
    if (containsTriple("<$uri>", "a", "?o")) {
        getResource(uri)
    } else {
        try {
            RDFDataMgr.loadModel(uri, Lang.TURTLE).getResource(uri)
        } catch (ex: Exception) {
            null
        }
    }

fun Resource.addPropertiesFromOrgResource(orgResource: Resource?) {
    if (orgResource != null) {
        safeAddProperty(RDF.type, orgResource.getProperty(RDF.type)?.`object`)
        safeAddProperty(DCTerms.identifier, orgResource.getProperty(DCTerms.identifier)?.`object`)
        safeAddProperty(BR.orgPath, orgResource.getProperty(BR.orgPath)?.`object`)
        safeAddProperty(ROV.legalName, orgResource.getProperty(ROV.legalName)?.`object`)
        safeAddProperty(FOAF.name, orgResource.getProperty(FOAF.name)?.`object`)
        safeAddProperty(ROV.orgType, orgResource.getProperty(ROV.orgType)?.`object`)
    }
}

fun Resource.safeAddProperty(property: Property, value: RDFNode?): Resource =
    if (value == null) this
    else addProperty(property, value)

fun Model.extreactQualifiedAttributionAgents(): List<Resource> =
    listResourcesWithProperty(PROV.qualifiedAttribution)
        .toList()
        .flatMap { it.listProperties(PROV.qualifiedAttribution).toList() }
        .asSequence()
        .filter { it.isResourceProperty() }
        .map { it.resource }
        .flatMap { it.listProperties(PROV.agent).toList() }
        .filter { it.isResourceProperty() }
        .map { it.resource }
        .toList()

fun Model.extractOrganizations(organizationsPredicates: List<Property>): List<Resource> =
    organizationsPredicates.flatMap { organizationPredicate ->
        listResourcesWithProperty(organizationPredicate)
            .toList()
            .flatMap { it.listProperties(organizationPredicate).toList() }
            .asSequence()
            .filter { it.isResourceProperty() }
            .map { it.resource }
            .toList()
    }

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

fun Resource.dctIdentifierIsInadequate(): Boolean =
    listProperties(DCTerms.identifier)
        .toList()
        .map { it.`object` }
        .mapNotNull { it.extractOrganizationId() }
        .isEmpty()

fun RDFNode.extractOrganizationId(): String? =
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
    "data.norge.no/nlod",
    "publications.europa.eu/resource/authority/licence/CC0",
    "publications.europa.eu/resource/authority/licence/NLOD_2_0",
    "publications.europa.eu/resource/authority/licence/CC_BY_4_0")

fun formatNowWithOsloTimeZone(): String =
    ZonedDateTime.now(ZoneId.of("Europe/Oslo"))
        .format(DateTimeFormatter.ofPattern(dateFormat))

fun Date.formatWithOsloTimeZone(): String =
    ZonedDateTime.from(toInstant().atZone(ZoneId.of("Europe/Oslo")))
        .format(DateTimeFormatter.ofPattern(dateFormat))
