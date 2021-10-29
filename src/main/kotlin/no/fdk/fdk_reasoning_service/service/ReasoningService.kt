package no.fdk.fdk_reasoning_service.service

import kotlinx.coroutines.*
import no.fdk.fdk_reasoning_service.config.ApplicationURI
import no.fdk.fdk_reasoning_service.model.CatalogType
import no.fdk.fdk_reasoning_service.rdf.CV
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.reasoner.rulesys.GenericRuleReasoner
import org.apache.jena.reasoner.rulesys.Rule
import org.apache.jena.riot.Lang
import org.apache.jena.riot.RDFDataMgr
import org.apache.jena.vocabulary.DCTerms
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.Executors

private val LOGGER: Logger = LoggerFactory.getLogger(ReasoningService::class.java)

@Service
class ReasoningService(
    private val uris: ApplicationURI
) : CoroutineScope by CoroutineScope(Executors.newFixedThreadPool(10).asCoroutineDispatcher()) {

    fun catalogReasoning(catalogType: CatalogType): Model {
        LOGGER.debug("Starting $catalogType reasoning")

        val rdfData = listOf(
            async {
                try {
                    RDFDataMgr.loadModel(catalogType.uri(uris), Lang.TURTLE)
                } catch (ex: Exception) {
                    LOGGER.error("Download failed for ${catalogType.uri(uris)}", ex)
                    ModelFactory.createDefaultModel()
                }
            },
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
            async { catalogType.extendedPublishersModel(orgData = rdfData[1], catalogData = rdfData[0]) },
            async { catalogType.deductionsModel(catalogData = rdfData[0], losData = rdfData[2]) }
        ).let { runBlocking { it.awaitAll() } }

        return rdfData[0].union(deductions[0]).union(deductions[1])
    }

    private fun CatalogType.extendedPublishersModel(orgData: Model, catalogData: Model): Model {
        val publisherPredicate = when (this) {
            CatalogType.EVENTS -> CV.hasCompetentAuthority
            CatalogType.PUBLICSERVICES -> CV.hasCompetentAuthority
            else -> DCTerms.publisher
        }
        return orgData.createModelOfPublishersWithOrgData(
            publisherURIs = catalogData.extractInadequatePublishers(publisherPredicate),
            orgsURI = uris.organizations
        )
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
