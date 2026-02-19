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
        val msg = getKafkaEvent(fdkId, graph, timestamp, resourceType, harvestRunId, uri)
        LOGGER.info("Sending reasoned event topic={} msg={}", topicName, formatRecordForLog(msg))
        kafkaTemplate.send(topicName, msg)
        return true
    }

    private fun formatRecordForLog(record: SpecificRecord): String {
        return record.schema.fields.joinToString(", ") { field ->
            val value = record.get(field.pos())
            val str = when (value) {
                is CharSequence -> if (value.length > 80) "${value.toString().take(80)}...(${value.length} chars)" else value.toString()
                else -> value?.toString() ?: "null"
            }
            "${field.name()}=$str"
        }
    }

    /**
     * Kotlin can pass a "non-null" String that is null at runtime (e.g. from Java/GenericRecord).
     * The Java Avro builder then stores null and serialization fails. Force a non-null value for
     * required fields by copying through a fresh String so the JVM never sees null.
     */
    private fun requireNonBlank(value: String?, name: String): String =
        (value?.takeIf { it.isNotBlank() } ?: throw IllegalArgumentException("$name must not be null or blank"))

    private fun getKafkaEvent(
        fdkId: String?,
        graph: String?,
        timestamp: Long,
        resourceType: CatalogType,
        harvestRunId: String?,
        uri: String?,
    ): SpecificRecord {
        val safeFdkId = requireNonBlank(fdkId, "fdkId")
        val safeGraph = requireNonBlank(graph, "graph")
        return when (resourceType) {
            CatalogType.DATASETS -> DatasetEvent.newBuilder()
                .setType(DatasetEventType.DATASET_REASONED)
                .setHarvestRunId(harvestRunId)
                .setUri(uri)
                .setFdkId(safeFdkId)
                .setGraph(safeGraph)
                .setTimestamp(timestamp)
                .build()
            CatalogType.CONCEPTS -> ConceptEvent.newBuilder()
                .setType(ConceptEventType.CONCEPT_REASONED)
                .setHarvestRunId(harvestRunId)
                .setUri(uri)
                .setFdkId(safeFdkId)
                .setGraph(safeGraph)
                .setTimestamp(timestamp)
                .build()
            CatalogType.DATASERVICES -> DataServiceEvent.newBuilder()
                .setType(DataServiceEventType.DATA_SERVICE_REASONED)
                .setHarvestRunId(harvestRunId)
                .setUri(uri)
                .setFdkId(safeFdkId)
                .setGraph(safeGraph)
                .setTimestamp(timestamp)
                .build()
            CatalogType.INFORMATIONMODELS -> InformationModelEvent.newBuilder()
                .setType(InformationModelEventType.INFORMATION_MODEL_REASONED)
                .setHarvestRunId(harvestRunId)
                .setUri(uri)
                .setFdkId(safeFdkId)
                .setGraph(safeGraph)
                .setTimestamp(timestamp)
                .build()
            CatalogType.PUBLICSERVICES -> ServiceEvent.newBuilder()
                .setType(ServiceEventType.SERVICE_REASONED)
                .setHarvestRunId(harvestRunId)
                .setUri(uri)
                .setFdkId(safeFdkId)
                .setGraph(safeGraph)
                .setTimestamp(timestamp)
                .build()
            CatalogType.EVENTS -> EventEvent.newBuilder()
                .setType(EventEventType.EVENT_REASONED)
                .setHarvestRunId(harvestRunId)
                .setUri(uri)
                .setFdkId(safeFdkId)
                .setGraph(safeGraph)
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
