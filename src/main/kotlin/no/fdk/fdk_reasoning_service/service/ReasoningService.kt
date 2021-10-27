package no.fdk.fdk_reasoning_service.service

import no.fdk.fdk_reasoning_service.config.ApplicationURI
import no.fdk.fdk_reasoning_service.model.CatalogType
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.reasoner.rulesys.GenericRuleReasoner
import org.apache.jena.reasoner.rulesys.Rule
import org.apache.jena.riot.Lang
import org.apache.jena.riot.RDFDataMgr
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

private val LOGGER: Logger = LoggerFactory.getLogger(ReasoningService::class.java)

@Service
class ReasoningService(
    private val uris: ApplicationURI
) {
    fun catalogReasoning(catalogType: CatalogType): Model {
        LOGGER.debug("Starting $catalogType reasoning")
        val catalog = RDFDataMgr.loadModel(catalogType.uri(uris), Lang.TURTLE)
        val orgs = RDFDataMgr.loadModel(uris.organizations, Lang.TURTLE)

        val publishers = orgs?.createModelOfPublishersWithOrgData(
            catalog?.extractInadequatePublishers() ?: emptySet(), uris.organizations
        )

        val reasoner = GenericRuleReasoner(Rule.parseRules(datasetRules))
        val infModel = ModelFactory.createInfModel(reasoner, catalog)
        infModel.fdkPrefix()

        return infModel.union(publishers)
    }
}
