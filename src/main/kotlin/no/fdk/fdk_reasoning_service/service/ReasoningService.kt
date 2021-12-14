package no.fdk.fdk_reasoning_service.service

import kotlinx.coroutines.*
import no.fdk.fdk_reasoning_service.config.ApplicationURI
import no.fdk.fdk_reasoning_service.model.CatalogType
import no.fdk.fdk_reasoning_service.rdf.BR
import no.fdk.fdk_reasoning_service.rdf.CV
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
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
import java.util.concurrent.Executors

private val LOGGER: Logger = LoggerFactory.getLogger(ReasoningService::class.java)

@Service
class ReasoningService(
    private val uris: ApplicationURI
) : CoroutineScope by CoroutineScope(Executors.newFixedThreadPool(10).asCoroutineDispatcher()) {

    fun catalogReasoning(catalogModel: Model, catalogType: CatalogType): Model {
        LOGGER.debug("Starting $catalogType reasoning")

        val rdfData = listOf(
            async {
                try {
                    RDFDataMgr.loadModel(uris.organizations, Lang.TURTLE)
                } catch (ex: Exception) {
                    LOGGER.error("Download failed for ${uris.organizations}", ex)
                    ModelFactory.createDefaultModel()
                }
            },
            async {
                try {
                    RDFDataMgr.loadModel(uris.los, Lang.RDFXML)
                } catch (ex: Exception) {
                    LOGGER.error("Download failed for ${uris.los}", ex)
                    ModelFactory.createDefaultModel()
                }
            }
        ).let { runBlocking { it.awaitAll() } }

        val deductions = listOf(
            async { catalogType.extendedPublishersModel(orgData = rdfData[0], catalogData = catalogModel) },
            async { catalogType.deductionsModel(catalogData = catalogModel, losData = rdfData[1]) }
        ).let { runBlocking { it.awaitAll() } }

        return catalogModel.union(deductions[0]).union(deductions[1])
    }

    private fun CatalogType.extendedPublishersModel(orgData: Model, catalogData: Model): Model {
        val publisherPredicate = when (this) {
            CatalogType.EVENTS -> CV.hasCompetentAuthority
            CatalogType.PUBLICSERVICES -> CV.hasCompetentAuthority
            else -> DCTerms.publisher
        }
        val publisherResources = if (this == CatalogType.DATASETS) {
                catalogData.extractPublisherURIs(publisherPredicate).plus(catalogData.extreactQualifiedAttributionAgentURIs())
            } else catalogData.extractPublisherURIs(publisherPredicate)
        return orgData.createModelOfPublishersWithOrgData(
            publisherURIs = publisherResources
                .filter { it.dctIdentifierIsInadequate() }
                .mapNotNull { it.uri }
                .toSet(),
            orgsURI = uris.organizations
        ).addOrgPathWhenMissing(publisherResources.mapNotNull { it.uri }.toSet(), catalogData)
    }

    private fun Model.addOrgPathWhenMissing(publisherURIs: Set<String>, catalogData: Model): Model {
        publisherURIs
            .filterNot { containsTriple("<$it>", "<${BR.orgPath.uri}>", "?o") }
            .filterNot { catalogData.containsTriple("<$it>", "<${BR.orgPath.uri}>", "?o") }
            .map { Triple(it, catalogData.dctIdentifierIfOrgId(it), catalogData.foafName(it)) }
            .forEach {
                getOrgPath(it.second, it.third)
                    ?.let { orgPath -> getResource(it.first).addProperty(BR.orgPath, orgPath) }
            }
        return this
    }

    private fun Model.dctIdentifierIfOrgId(uri: String): String? {
        val orgId: String? = getProperty(getResource(uri), DCTerms.identifier)?.string
        val regex = Regex("""^[0-9]{9}$""")
        val matching = regex.findAll(orgId ?: "").toList()

        return if (matching.size == 1) orgId
        else null
    }

    private fun Model.foafName(uri: String): String? {
        val names = getResource(uri)?.listProperties(FOAF.name)?.toList()
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

    private fun orgPathAdapter(value: String): String? {
        val uri = "${uris.organizations}/orgpath/$value"
        with(URL(uri).openConnection() as HttpURLConnection) {
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

    private fun CatalogType.deductionsModel(catalogData: Model, losData: Model): Model =
        when (this) {
            CatalogType.DATASETS -> ModelFactory.createInfModel(
                GenericRuleReasoner(Rule.parseRules(datasetRules)).bindSchema(losData),
                catalogData.fdkPrefix()
            ).deductionsModel
            else -> ModelFactory.createDefaultModel()
        }
}
