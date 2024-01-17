package no.fdk.fdk_reasoning_service.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import no.fdk.fdk_reasoning_service.cache.ReferenceDataCache
import no.fdk.fdk_reasoning_service.model.CatalogType
import no.fdk.fdk_reasoning_service.model.ExternalRDFData
import no.fdk.fdk_reasoning_service.model.HarvestReport
import no.fdk.fdk_reasoning_service.model.RetryReportsWrap
import no.fdk.fdk_reasoning_service.rabbit.RabbitMQPublisher
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.*

val RETRY_QUEUE: Queue<RetryReportsWrap> = LinkedList()
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
    private val referenceDataCache: ReferenceDataCache
) : CoroutineScope by CoroutineScope(Dispatchers.Default) {

    @Scheduled(fixedRate = 60000)
    private fun pollQueue() =
        RETRY_QUEUE.poll()?.run { initiateReasoning(type, reports, retryCount) }

    fun initiateReasoning(type: CatalogType, reports: List<HarvestReport>, retryCount: Int = 0) {
        val start = Date()
        val rdfData = ExternalRDFData(
            orgData = referenceDataCache.organizations(),
            losData = referenceDataCache.los(),
            eurovocs = referenceDataCache.eurovocs(),
            dataThemes = referenceDataCache.dataThemes(),
            conceptStatuses = referenceDataCache.conceptStatuses(),
            conceptSubjects = referenceDataCache.conceptSubjects(),
            ianaMediaTypes = referenceDataCache.ianaMediaTypes(),
            fileTypes = referenceDataCache.fileTypes(),
            openLicenses = referenceDataCache.openLicenses(),
            linguisticSystems = referenceDataCache.linguisticSystems(),
            locations = referenceDataCache.locations(),
            accessRights = referenceDataCache.accessRights(),
            frequencies = referenceDataCache.frequencies(),
            provenance = referenceDataCache.provenance(),
            publisherTypes = referenceDataCache.publisherTypes(),
            admsStatuses = referenceDataCache.admsStatuses()
        )
        try {
            when {
                rdfData.orgData.isEmpty -> throw Exception("missing org data")
                rdfData.losData.isEmpty -> throw Exception("missing los data")
                rdfData.eurovocs.isEmpty -> throw Exception("missing eurovocs data")
                rdfData.dataThemes.isEmpty -> throw Exception("missing data themes")
                else -> launchReasoning(type, start, reports, rdfData, retryCount)
            }
        } catch (ex: Exception) {
            LOGGER.warn("reasoning activity $type was aborted: ${ex.message}")
            queueRetry(type, reports, retryCount)
        }
    }

    private fun launchReasoning(
        type: CatalogType,
        start: Date,
        reports: List<HarvestReport>,
        rdfData: ExternalRDFData,
        retryCount: Int
    ) = launch {
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
            queueRetry(type, reports, retryCount)
        }
    }

    private fun queueRetry(type: CatalogType, reports: List<HarvestReport>, retryCount: Int) {
        if (retryCount < 10) RETRY_QUEUE.add(RetryReportsWrap(type, retryCount + 1, reports))
        else {
            val catalogs = reports.flatMap { report -> report.changedCatalogs.map { catalog -> catalog.fdkId } }
            LOGGER.error("reasoning of $type failed too many times, aborting completely. Catalogs affected: $catalogs")
        }
    }

}
