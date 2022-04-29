package no.fdk.fdk_reasoning_service.service

import no.fdk.fdk_reasoning_service.Application
import no.fdk.fdk_reasoning_service.model.CatalogType
import no.fdk.fdk_reasoning_service.model.TurtleDBO
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
import org.apache.jena.vocabulary.SKOS
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.io.StringReader
import java.util.*
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

private val logger = LoggerFactory.getLogger(Application::class.java)
const val UNION_ID = "union-graph"

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

fun gzip(content: String): String {
    val bos = ByteArrayOutputStream()
    GZIPOutputStream(bos).bufferedWriter(Charsets.UTF_8).use { it.write(content) }
    return Base64.getEncoder().encodeToString(bos.toByteArray())
}

fun ungzip(base64Content: String): String {
    val content = Base64.getDecoder().decode(base64Content)
    return GZIPInputStream(content.inputStream())
        .bufferedReader(Charsets.UTF_8)
        .use { it.readText() }
}

fun Model.fdkPrefix(): Model =
    setNsPrefix("fdk", "https://raw.githubusercontent.com/Informasjonsforvaltning/fdk-reasoning-service/master/src/main/resources/ontology/fdk.owl#")

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
        ?.let { downloadOrgDataIfMissing("$orgsURI/${orgIdFromURI(publisherURI)}") }

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
        .asSequence()
        .filter { it.isResourceProperty() }
        .map { it.resource }
        .toList()

fun Model.extractPublishers(publisherPredicate: Property): List<Resource> =
    listResourcesWithProperty(publisherPredicate)
        .toList()
        .flatMap { it.listProperties(publisherPredicate).toList() }
        .asSequence()
        .filter { it.isResourceProperty() }
        .map { it.resource }
        .toList()

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

fun Resource.catalogRecordURI(): String? {
    val selectQuery = "SELECT ?record { ?record <http://xmlns.com/foaf/0.1/primaryTopic> <$uri> . } LIMIT 1"

    return try {
        val query = QueryFactory.create(selectQuery)
        return QueryExecutionFactory.create(query, model).execSelect()
            .asSequence()
            .firstOrNull()
            ?.get("record")
            ?.toString()
    } catch (ex: Exception) {
        logger.error("Unable to find record for $uri", ex)
        null
    }
}

fun Resource.fdkId(recordURI: String): String? {
    val selectQuery = "SELECT ?fdkId { <$recordURI> <http://purl.org/dc/terms/identifier> ?fdkId . } LIMIT 1"

    return try {
        val query = QueryFactory.create(selectQuery)
        return QueryExecutionFactory.create(query, model).execSelect()
            .asSequence()
            .firstOrNull()
            ?.get("fdkId")
            ?.toString()
    } catch (ex: Exception) {
        logger.error("Unable to find fdkId for $recordURI", ex)
        null
    }
}

fun Resource.catalogRecordModel(recordURI: String): Model =
    model.getResource(recordURI).listProperties().toModel()

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

fun TurtleDBO.toRDF(lang: Lang): String? =
    ungzip(turtle)
        .let {
            if (lang == Lang.TURTLE) it
            else parseRDFResponse(it, Lang.TURTLE, null)?.createRDFResponse(lang)
        }

fun Model.createUnionDBO(): TurtleDBO =
    TurtleDBO(
        id = UNION_ID,
        turtle = gzip(createRDFResponse(Lang.TURTLE))
    )

fun Model.createDBO(fdkId: String): TurtleDBO =
    TurtleDBO(
        id = fdkId,
        turtle = gzip(createRDFResponse(Lang.TURTLE))
    )

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

fun Resource.extractFDKIdAndRecordURI(): FDKIdAndRecordURI? {

    val recordURI = catalogRecordURI()
    if (recordURI == null) {
        logger.error("Unable to find record for $uri", Exception())
        return null
    }

    val fdkId = fdkId(recordURI)
    if (fdkId == null) {
        logger.error("Unable to find fdkId for $recordURI", Exception())
        return null
    }

    return FDKIdAndRecordURI(fdkId, recordURI)
}

data class FDKIdAndRecordURI(
    val fdkId: String,
    val recordURI: String
)

fun catalogTypeFromRabbitMessageKey(key: String): CatalogType? =
    when {
        key.contains("concepts") -> CatalogType.CONCEPTS
        key.contains("dataservices") -> CatalogType.DATASERVICES
        key.contains("datasets") -> CatalogType.DATASETS
        key.contains("informationmodels") -> CatalogType.INFORMATIONMODELS
        key.contains("events") -> CatalogType.EVENTS
        key.contains("public_services") -> CatalogType.PUBLICSERVICES
        else -> null
    }
