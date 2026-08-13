package no.fdk.reasoning.kafka

import no.fdk.harvest.HarvestEvent
import no.fdk.harvest.HarvestPhase
import no.fdk.reasoning.metrics.ReasonedEventMetrics
import no.fdk.reasoning.metrics.ReasonedEventMetrics.PublishKind
import no.fdk.reasoning.metrics.ReasonedEventMetrics.PublishOutcome
import no.fdk.reasoning.model.CatalogType
import org.apache.avro.specific.SpecificRecord
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class KafkaHarvestEventProducer(private val kafkaTemplate: KafkaTemplate<String, SpecificRecord>) {
    fun sendReasoningSuccessEvent(
        harvestRunId: String?,
        catalogType: CatalogType,
        fdkId: String?,
        resourceUri: String?,
        startTime: Instant,
        endTime: Instant,
    ) {
        if (harvestRunId == null) {
            LOGGER.debug("Skipping harvest event - harvestRunId is null")
            return
        }

        val harvestEvent =
            HarvestEvent
                .newBuilder()
                .setPhase(HarvestPhase.REASONING)
                .setRunId(harvestRunId)
                .setDataType(mapCatalogTypeToDataType(catalogType))
                .setFdkId(fdkId)
                .setResourceUri(resourceUri)
                .setStartTime(startTime.toString())
                .setEndTime(endTime.toString())
                .setDataSourceId(null)
                .setDataSourceUrl(null)
                .setAcceptHeader(null)
                .setErrorMessage(null)
                .setChangedResourcesCount(null)
                .setRemovedResourcesCount(null)
                .build()

        LOGGER.debug("Sending reasoning success harvest event for runId: $harvestRunId, fdkId: $fdkId")
        publishHarvestEvent(catalogType, harvestEvent)
    }

    fun sendReasoningFailureEvent(
        harvestRunId: String?,
        catalogType: CatalogType,
        fdkId: String?,
        resourceUri: String?,
        startTime: Instant,
        endTime: Instant,
        errorMessage: String,
    ) {
        if (harvestRunId == null) {
            LOGGER.debug("Skipping harvest event - harvestRunId is null")
            return
        }

        val harvestEvent =
            HarvestEvent
                .newBuilder()
                .setPhase(HarvestPhase.REASONING)
                .setRunId(harvestRunId)
                .setDataType(mapCatalogTypeToDataType(catalogType))
                .setFdkId(fdkId)
                .setResourceUri(resourceUri)
                .setStartTime(startTime.toString())
                .setEndTime(endTime.toString())
                .setErrorMessage(errorMessage)
                .setDataSourceId(null)
                .setDataSourceUrl(null)
                .setAcceptHeader(null)
                .setChangedResourcesCount(null)
                .setRemovedResourcesCount(null)
                .build()

        LOGGER.debug("Sending reasoning failure harvest event for runId: $harvestRunId, fdkId: $fdkId, error: $errorMessage")
        publishHarvestEvent(catalogType, harvestEvent)
    }

    private fun publishHarvestEvent(catalogType: CatalogType, harvestEvent: HarvestEvent) {
        try {
            kafkaTemplate
                .send(TOPIC_NAME_HARVEST, harvestEvent)
                .whenComplete { _, ex ->
                    ReasonedEventMetrics.recordPublish(
                        catalogType = catalogType,
                        kind = PublishKind.HARVEST,
                        outcome = if (ex == null) {
                            PublishOutcome.SUCCESS
                        } else {
                            PublishOutcome.PUBLISH_FAILED
                        },
                    )
                    if (ex != null) {
                        LOGGER.error(
                            "Failed to produce harvest event for runId={} catalogType={}",
                            harvestEvent.runId,
                            catalogType,
                            ex,
                        )
                    }
                }
        } catch (e: Exception) {
            ReasonedEventMetrics.recordPublish(catalogType, PublishKind.HARVEST, PublishOutcome.PUBLISH_FAILED)
            LOGGER.error("Failed to enqueue harvest event for catalogType={}", catalogType, e)
            throw e
        }
    }

    private fun mapCatalogTypeToDataType(catalogType: CatalogType): no.fdk.harvest.DataType = when (catalogType) {
        CatalogType.DATASETS -> no.fdk.harvest.DataType.dataset
        CatalogType.CONCEPTS -> no.fdk.harvest.DataType.concept
        CatalogType.INFORMATIONMODELS -> no.fdk.harvest.DataType.informationmodel
        CatalogType.DATASERVICES -> no.fdk.harvest.DataType.dataservice
        CatalogType.PUBLICSERVICES -> no.fdk.harvest.DataType.publicService
        CatalogType.EVENTS -> no.fdk.harvest.DataType.event
    }

    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(KafkaHarvestEventProducer::class.java)
        private const val TOPIC_NAME_HARVEST = "harvest-events"
    }
}
