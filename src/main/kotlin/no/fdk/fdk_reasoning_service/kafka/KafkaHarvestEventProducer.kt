package no.fdk.fdk_reasoning_service.kafka

import no.fdk.fdk_reasoning_service.model.CatalogType
import no.fdk.harvest.HarvestEvent
import no.fdk.harvest.HarvestPhase
import org.apache.avro.specific.SpecificRecord
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class KafkaHarvestEventProducer(
    private val kafkaTemplate: KafkaTemplate<String, SpecificRecord>,
) {
    fun sendReasoningSuccessEvent(
        harvestRunId: String?,
        catalogType: CatalogType,
        fdkId: String?,
        resourceUri: String?,
        eventTimestamp: Long,
        startTime: Instant,
        endTime: Instant,
    ) {
        if (harvestRunId == null) {
            LOGGER.debug("Skipping harvest event - harvestRunId is null")
            return
        }

        val harvestEvent = HarvestEvent.newBuilder()
            .setPhase(HarvestPhase.REASONING)
            .setRunId(harvestRunId)
            .setDataType(mapCatalogTypeToDataType(catalogType))
            .setFdkId(fdkId)
            .setResourceUri(resourceUri)
            .setTimestamp(eventTimestamp)
            .setStartTime(startTime.toString())
            .setEndTime(endTime.toString())
            .setDataSourceId(null)
            .setDataSourceUrl(null)
            .setAcceptHeader(null)
            .setErrorMessage(null)
            .setChangedResourcesCount(null)
            .setUnchangedResourcesCount(null)
            .setRemovedResourcesCount(null)
            .build()

        LOGGER.debug("Sending reasoning success harvest event for runId: $harvestRunId, fdkId: $fdkId")
        kafkaTemplate.send(TOPIC_NAME_HARVEST, harvestEvent)
    }

    fun sendReasoningFailureEvent(
        harvestRunId: String?,
        catalogType: CatalogType,
        fdkId: String?,
        resourceUri: String?,
        eventTimestamp: Long,
        startTime: Instant,
        endTime: Instant,
        errorMessage: String,
    ) {
        if (harvestRunId == null) {
            LOGGER.debug("Skipping harvest event - harvestRunId is null")
            return
        }

        val harvestEvent = HarvestEvent.newBuilder()
            .setPhase(HarvestPhase.REASONING)
            .setRunId(harvestRunId)
            .setDataType(mapCatalogTypeToDataType(catalogType))
            .setFdkId(fdkId)
            .setResourceUri(resourceUri)
            .setTimestamp(eventTimestamp)
            .setStartTime(startTime.toString())
            .setEndTime(endTime.toString())
            .setErrorMessage(errorMessage)
            .setDataSourceId(null)
            .setDataSourceUrl(null)
            .setAcceptHeader(null)
            .setChangedResourcesCount(null)
            .setUnchangedResourcesCount(null)
            .setRemovedResourcesCount(null)
            .build()

        LOGGER.debug("Sending reasoning failure harvest event for runId: $harvestRunId, fdkId: $fdkId, error: $errorMessage")
        kafkaTemplate.send(TOPIC_NAME_HARVEST, harvestEvent)
    }

    private fun mapCatalogTypeToDataType(catalogType: CatalogType): no.fdk.harvest.DataType {
        return when (catalogType) {
            CatalogType.DATASETS -> no.fdk.harvest.DataType.dataset
            CatalogType.CONCEPTS -> no.fdk.harvest.DataType.concept
            CatalogType.INFORMATIONMODELS -> no.fdk.harvest.DataType.informationmodel
            CatalogType.DATASERVICES -> no.fdk.harvest.DataType.dataservice
            CatalogType.PUBLICSERVICES -> no.fdk.harvest.DataType.publicService
            CatalogType.EVENTS -> no.fdk.harvest.DataType.event
        }
    }

    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(KafkaHarvestEventProducer::class.java)
        private const val TOPIC_NAME_HARVEST = "harvest-events"
    }
}

