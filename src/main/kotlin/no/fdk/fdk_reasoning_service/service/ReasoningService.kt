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
            async { RDFDataMgr.loadModel(catalogType.uri(uris), Lang.TURTLE) },
            async { RDFDataMgr.loadModel(uris.organizations, Lang.TURTLE) }
        ).let { runBlocking { it.awaitAll() } }

        val models = listOf(
            async {
                catalogType.extendedPublishersModel(orgData = rdfData[1], catalogData = rdfData[0])
                    ?: ModelFactory.createDefaultModel()
            },
            async { catalogType.infModel(rdfData[0]) }
        ).let { runBlocking { it.awaitAll() } }

        return models[0].union(models[1])
    }

    private fun CatalogType.extendedPublishersModel(orgData: Model?, catalogData: Model?): Model? {
        val publisherPredicate = when (this) {
            CatalogType.EVENTS -> CV.hasCompetentAuthority
            CatalogType.PUBLICSERVICES -> CV.hasCompetentAuthority
            else -> DCTerms.publisher
        }
        return orgData?.createModelOfPublishersWithOrgData(
            publisherURIs = catalogData?.extractInadequatePublishers(publisherPredicate)
                ?: emptySet(),
            orgsURI = uris.organizations
        )
    }

    private fun CatalogType.infModel(rdfData: Model): Model =
        when (this) {
            CatalogType.DATASETS -> ModelFactory.createInfModel(
                GenericRuleReasoner(Rule.parseRules(datasetRules)),
                rdfData.fdkPrefix()
            )
            else -> rdfData
        }
}
