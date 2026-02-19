package no.fdk.fdk_reasoning_service.kafka

import no.fdk.concept.ConceptEvent
import no.fdk.concept.ConceptEventType
import no.fdk.dataservice.DataServiceEvent
import no.fdk.dataservice.DataServiceEventType
import no.fdk.dataset.DatasetEvent
import no.fdk.dataset.DatasetEventType
import no.fdk.event.EventEvent
import no.fdk.event.EventEventType
import no.fdk.fdk_reasoning_service.model.CatalogType
import no.fdk.informationmodel.InformationModelEvent
import no.fdk.informationmodel.InformationModelEventType
import no.fdk.service.ServiceEvent
import no.fdk.service.ServiceEventType
import org.apache.avro.specific.SpecificRecord
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
class KafkaReasonedEventProducer(
    private val kafkaTemplate: KafkaTemplate<String, SpecificRecord>,
) {
    /**
     * Sends a reasoned event to the appropriate topic.
     * @return true if the message was sent, false if skipped (e.g. fdkId or graph null/blank)
     */
    fun sendMessage(
        fdkId: String?,
        graph: String?,
        timestamp: Long,
        resourceType: CatalogType,
        harvestRunId: String? = null,
        uri: String? = null,
    ): Boolean {
        if (fdkId.isNullOrBlank() || graph.isNullOrBlank()) {
            LOGGER.warn("Skipping reasoned event send: fdkId or graph is null or blank (resourceType={})", resourceType)
            return false
        }
        val topicName =
            when (resourceType) {
                CatalogType.DATASETS -> TOPIC_NAME_DATASET
                CatalogType.CONCEPTS -> TOPIC_NAME_CONCEPT
                CatalogType.DATASERVICES -> TOPIC_NAME_DATA_SERVICE
                CatalogType.INFORMATIONMODELS -> TOPIC_NAME_INFORMATION_MODEL
                CatalogType.PUBLICSERVICES -> TOPIC_NAME_SERVICE
                CatalogType.EVENTS -> TOPIC_NAME_EVENT
            }
        val msg = getKafkaEvent(fdkId!!, graph!!, timestamp, resourceType, harvestRunId, uri)
        LOGGER.debug("Sending message with fdkId $fdkId to Kafka topic: $topicName")
        kafkaTemplate.send(topicName, msg)
        return true
    }

    private fun getKafkaEvent(
        fdkId: String,
        graph: String,
        timestamp: Long,
        resourceType: CatalogType,
        harvestRunId: String?,
        uri: String?,
    ): SpecificRecord {
        require(fdkId.isNotBlank()) { "fdkId must not be null or blank" }
        require(graph.isNotBlank()) { "graph must not be null or blank" }
        return when (resourceType) {
            CatalogType.DATASETS -> DatasetEvent.newBuilder()
                .setType(DatasetEventType.DATASET_REASONED)
                .setHarvestRunId(harvestRunId)
                .setUri(uri)
                .setFdkId(fdkId)
                .setGraph(graph)
                .setTimestamp(timestamp)
                .build()
            CatalogType.CONCEPTS -> ConceptEvent.newBuilder()
                .setType(ConceptEventType.CONCEPT_REASONED)
                .setHarvestRunId(harvestRunId)
                .setUri(uri)
                .setFdkId(fdkId)
                .setGraph(graph)
                .setTimestamp(timestamp)
                .build()
            CatalogType.DATASERVICES -> DataServiceEvent.newBuilder()
                .setType(DataServiceEventType.DATA_SERVICE_REASONED)
                .setHarvestRunId(harvestRunId)
                .setUri(uri)
                .setFdkId(fdkId)
                .setGraph(graph)
                .setTimestamp(timestamp)
                .build()
            CatalogType.INFORMATIONMODELS -> InformationModelEvent.newBuilder()
                .setType(InformationModelEventType.INFORMATION_MODEL_REASONED)
                .setHarvestRunId(harvestRunId)
                .setUri(uri)
                .setFdkId(fdkId)
                .setGraph(graph)
                .setTimestamp(timestamp)
                .build()
            CatalogType.PUBLICSERVICES -> ServiceEvent.newBuilder()
                .setType(ServiceEventType.SERVICE_REASONED)
                .setHarvestRunId(harvestRunId)
                .setUri(uri)
                .setFdkId(fdkId)
                .setGraph(graph)
                .setTimestamp(timestamp)
                .build()
            CatalogType.EVENTS -> EventEvent.newBuilder()
                .setType(EventEventType.EVENT_REASONED)
                .setHarvestRunId(harvestRunId)
                .setUri(uri)
                .setFdkId(fdkId)
                .setGraph(graph)
                .setTimestamp(timestamp)
                .build()
        }
    }

    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(KafkaReasonedEventProducer::class.java)
        private const val TOPIC_NAME_DATASET = "dataset-events"
        private const val TOPIC_NAME_CONCEPT = "concept-events"
        private const val TOPIC_NAME_DATA_SERVICE = "data-service-events"
        private const val TOPIC_NAME_INFORMATION_MODEL = "information-model-events"
        private const val TOPIC_NAME_SERVICE = "service-events"
        private const val TOPIC_NAME_EVENT = "event-events"
    }
}
