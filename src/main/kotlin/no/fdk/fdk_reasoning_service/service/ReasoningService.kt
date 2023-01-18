package no.fdk.fdk_reasoning_service.service

import no.fdk.fdk_reasoning_service.config.ApplicationURI
import no.fdk.fdk_reasoning_service.model.CatalogType
import no.fdk.fdk_reasoning_service.model.ExternalRDFData
import no.fdk.fdk_reasoning_service.rdf.BR
import no.fdk.fdk_reasoning_service.rdf.CV
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.Resource
import org.apache.jena.reasoner.rulesys.GenericRuleReasoner
import org.apache.jena.reasoner.rulesys.Rule
import org.apache.jena.riot.Lang
import org.apache.jena.riot.RDFDataMgr
import org.apache.jena.sparql.vocabulary.FOAF
import org.apache.jena.vocabulary.DCTerms
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL

private val LOGGER: Logger = LoggerFactory.getLogger(ReasoningService::class.java)

@Service
class ReasoningService(
    private val uris: ApplicationURI
) {

    fun catalogReasoning(catalogModel: Model, catalogType: CatalogType, rdfData: ExternalRDFData): Model =
        catalogModel
            .union(catalogType.extendedPublishersModel(orgData = rdfData.orgData, catalogData = catalogModel))
            .union(catalogType.deductionsModel(catalogData = catalogModel, losData = rdfData.losData))

    private fun CatalogType.extendedPublishersModel(orgData: Model, catalogData: Model): Model {
        val publisherPredicates = when (this) {
            CatalogType.EVENTS -> listOf(CV.hasCompetentAuthority)
            CatalogType.PUBLICSERVICES -> listOf(CV.hasCompetentAuthority, CV.ownedBy)
            else -> listOf(DCTerms.publisher)
        }
        val publisherResources = if (this == CatalogType.DATASETS) {
                catalogData.extractPublishers(publisherPredicates).plus(catalogData.extreactQualifiedAttributionAgents())
            } else catalogData.extractPublishers(publisherPredicates)
        return orgData.createModelOfPublishersWithOrgData(
            publisherURIs = publisherResources
                .filter { it.dctIdentifierIsInadequate() }
                .filter {  it.isURIResource }
                .mapNotNull { it.uri }
                .toSet(),
            orgsURI = uris.orgExternal
        ).addOrgPathAndNameWhenMissing(publisherResources.toSet(), catalogData, orgData)
    }

    private fun Model.addOrgPathAndNameWhenMissing(publishers: Set<Resource>, catalogData: Model, orgData: Model): Model {
        publishers.asSequence()
            .filterNot { containsTriple("<$it>", "<${FOAF.name.uri}>", "?o") }
            .filterNot { it.hasProperty(FOAF.name) }
            .map { Pair(it, catalogData.dctIdentifierIfOrgId(it)?.let { orgId -> orgData.getResource(orgURI(orgId)) }) }
            .filter { it.second != null }
            .forEach { it.first.safeAddProperty(FOAF.name, it.second?.getProperty(FOAF.name)?.`object`) }

        val publishersMissingOrgPath = publishers.asSequence()
            .filterNot { containsTriple("<$it>", "<${BR.orgPath.uri}>", "?o") }
            .filterNot { it.hasProperty(BR.orgPath) }

        publishersMissingOrgPath
            .map { Pair(it, catalogData.dctIdentifierIfOrgId(it)?.let { orgId -> orgData.getResource(orgURI(orgId)) }) }
            .filter { it.second != null }
            .forEach { it.first.safeAddProperty(BR.orgPath, it.second?.getProperty(BR.orgPath)?.`object`) }

        publishersMissingOrgPath
            .filterNot { containsTriple("<$it>", "<${BR.orgPath.uri}>", "?o") }
            .map { Triple(it, catalogData.dctIdentifierIfOrgId(it), it.foafName()) }
            .forEach { getOrgPath(it.second, it.third)?.let { orgPath -> it.first.addProperty(BR.orgPath, orgPath) } }

        return this
    }

    private fun Model.dctIdentifierIfOrgId(publisher: Resource): String? {
        val orgId: String? = getProperty(publisher, DCTerms.identifier)?.string
        val regex = Regex("""^[0-9]{9}$""")
        val matching = regex.findAll(orgId ?: "").toList()

        return if (matching.size == 1) orgId
        else null
    }

    private fun Resource.foafName(): String? {
        val names = listProperties(FOAF.name)?.toList()
        val nb = names?.find { it.language == "nb" }
        val nn = names?.find { it.language == "nn" }
        val en = names?.find { it.language == "en" }
        return when {
            names == null -> null
            names.isEmpty() -> null
            names.size == 1 -> names.first().string
            nb != null -> nb.string
            nn != null -> nn.string
            en != null -> en.string
            else -> names.first().string
        }
    }

    private fun getOrgPath(orgId: String?, orgName: String?): String? =
        when {
            orgId != null -> orgPathAdapter(orgId)
            orgName != null -> orgPathAdapter(orgName)
            else -> null
        }

    private fun orgURI(orgId: String) = "${uris.orgExternal}/$orgId"

    private fun orgPathAdapter(value: String): String? {
        val uri = "${uris.orgExternal}/orgpath/$value"
        with(URL(uri).openConnection() as HttpURLConnection) {
            setRequestProperty("Accept", "text/plain");
            try {
                if (HttpStatus.valueOf(responseCode).is2xxSuccessful) {
                    return inputStream.bufferedReader().use(BufferedReader::readText)
                } else {
                    LOGGER.error("Fetch of orgPath for value $value failed, status: $responseCode", Exception("Fetch of orgPath for value $value failed"))
                }
            } catch (ex: Exception) {
                LOGGER.error("Error fetching orgPath for value $value", ex)
            } finally {
                disconnect()
            }
            return null
        }
    }

    private fun orgModelAdapter(orgId: String?): Resource? =
        if (orgId != null) {
            val uri = "${uris.orgExternal}/$orgId"
            try {
                RDFDataMgr.loadModel(uri, Lang.TURTLE).getResource(uri)
            } catch (ex: Exception) {
                null
            }
        } else null

    private fun CatalogType.deductionsModel(catalogData: Model, losData: Model): Model =
        when (this) {
            CatalogType.CONCEPTS -> ModelFactory.createInfModel(
                GenericRuleReasoner(Rule.parseRules(conceptRules)),
                catalogData
            ).deductionsModel
            CatalogType.DATASETS -> ModelFactory.createInfModel(
                GenericRuleReasoner(Rule.parseRules(datasetRules)).bindSchema(losData),
                catalogData.fdkPrefix()
            ).deductionsModel
            CatalogType.DATASERVICES -> ModelFactory.createInfModel(
                GenericRuleReasoner(Rule.parseRules(dataServiceRules)),
                catalogData
            ).deductionsModel
            CatalogType.INFORMATIONMODELS -> ModelFactory.createInfModel(
                GenericRuleReasoner(Rule.parseRules(infoModelRules)),
                catalogData
            ).deductionsModel
            CatalogType.PUBLICSERVICES -> ModelFactory.createInfModel(
                GenericRuleReasoner(Rule.parseRules(serviceRules)),
                catalogData
            ).deductionsModel
            else -> ModelFactory.createDefaultModel()
        }
}
