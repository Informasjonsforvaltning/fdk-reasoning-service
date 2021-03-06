package no.fdk.fdk_reasoning_service.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import no.fdk.fdk_reasoning_service.config.ApplicationURI
import no.fdk.fdk_reasoning_service.model.CatalogType
import no.fdk.fdk_reasoning_service.model.ExternalRDFData
import no.fdk.fdk_reasoning_service.model.HarvestReport
import no.fdk.fdk_reasoning_service.rabbit.RabbitMQPublisher
import org.apache.jena.riot.Lang
import org.apache.jena.riot.RDFDataMgr
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.*

private val LOGGER: Logger = LoggerFactory.getLogger(ReasoningActivity::class.java)

@Component
class ReasoningActivity(
    private val conceptService: ConceptService,
    private val dataServiceService: DataServiceService,
    private val datasetService: DatasetService,
    private val eventService: EventService,
    private val infoModelService: InfoModelService,
    private val publicServiceService: PublicServiceService,
    private val rabbitMQPublisher: RabbitMQPublisher,
    private val uris: ApplicationURI
) : CoroutineScope by CoroutineScope(Dispatchers.Default) {

    fun initiateReasoning(key: String, reports: List<HarvestReport>) {
        val start = Date()
        val type = catalogTypeFromRabbitMessageKey(key)
        try {
            val rdfData = listOf(
                async {
                    try {
                        RDFDataMgr.loadModel(uris.organizations, Lang.TURTLE)
                    } catch (ex: Exception) {
                        LOGGER.error("Download failed for ${uris.organizations}", ex)
                        null
                    }
                },
                async {
                    try {
                        RDFDataMgr.loadModel(uris.los, Lang.RDFXML)
                    } catch (ex: Exception) {
                        LOGGER.error("Download failed for ${uris.los}", ex)
                        null
                    }
                }
            ).let { runBlocking { it.awaitAll() } }

            when {
                rdfData[0] == null -> throw Exception("missing org data")
                rdfData[1] == null -> throw Exception("missing los data")
                type == null -> throw Exception("invalid message key")
                else -> launchReasoning(
                    type, start, reports,
                    ExternalRDFData(orgData = rdfData[0]!!, losData = rdfData[1]!!))
            }
        } catch (ex: Exception) {
            LOGGER.warn("reasoning activity $type was aborted: ${ex.message}")
        }
    }

    private fun launchReasoning(type: CatalogType, start: Date, reports: List<HarvestReport>, rdfData: ExternalRDFData) = launch {
        try {
            reports
                .map {report ->
                    when (type) {
                        CatalogType.CONCEPTS -> async { conceptService.reasonReportedChanges(report, rdfData, start) }
                        CatalogType.DATASERVICES -> async { dataServiceService.reasonReportedChanges(report, rdfData, start) }
                        CatalogType.DATASETS -> async { datasetService.reasonReportedChanges(report, rdfData, start) }
                        CatalogType.EVENTS -> async { eventService.reasonReportedChanges(report, rdfData, start) }
                        CatalogType.INFORMATIONMODELS -> async { infoModelService.reasonReportedChanges(report, rdfData, start) }
                        CatalogType.PUBLICSERVICES -> async { publicServiceService.reasonReportedChanges(report, rdfData, start) }
                    } }
                .awaitAll()
                .also {
                    when (type) {
                        CatalogType.CONCEPTS -> conceptService.updateUnion()
                        CatalogType.DATASERVICES -> dataServiceService.updateUnion()
                        CatalogType.DATASETS -> datasetService.updateUnion()
                        CatalogType.EVENTS -> eventService.updateUnion()
                        CatalogType.INFORMATIONMODELS -> infoModelService.updateUnion()
                        CatalogType.PUBLICSERVICES -> publicServiceService.updateUnion()
                    } }
                .run { rabbitMQPublisher.send(type, this) }
        } catch (ex: Exception) {
            LOGGER.warn("reasoning activity $type was aborted: ${ex.message}")
        }
    }

}
