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
    fun sendMessage(
        fdkId: String,
        graph: String,
        timestamp: Long,
        resourceType: CatalogType,
    ) {
        val topicName =
            when (resourceType) {
                CatalogType.DATASETS -> TOPIC_NAME_DATASET
                CatalogType.CONCEPTS -> TOPIC_NAME_CONCEPT
                CatalogType.DATASERVICES -> TOPIC_NAME_DATA_SERVICE
                CatalogType.INFORMATIONMODELS -> TOPIC_NAME_INFORMATION_MODEL
                CatalogType.PUBLICSERVICES -> TOPIC_NAME_SERVICE
                CatalogType.EVENTS -> TOPIC_NAME_EVENT
            }
        val msg = getKafkaEvent(fdkId, graph, timestamp, resourceType)
        LOGGER.debug("Sending message to Kafka topic: $topicName")
        kafkaTemplate.send(topicName, msg)
    }

    private fun getKafkaEvent(
        fdkId: String,
        graph: String,
        timestamp: Long,
        resourceType: CatalogType,
    ): SpecificRecord =
        when (resourceType) {
            CatalogType.DATASETS -> DatasetEvent(DatasetEventType.DATASET_REASONED, fdkId, graph, timestamp)
            CatalogType.CONCEPTS -> ConceptEvent(ConceptEventType.CONCEPT_REASONED, fdkId, graph, timestamp)
            CatalogType.DATASERVICES ->
                DataServiceEvent(
                    DataServiceEventType.DATA_SERVICE_REASONED,
                    fdkId,
                    graph,
                    timestamp,
                )

            CatalogType.INFORMATIONMODELS ->
                InformationModelEvent(
                    InformationModelEventType.INFORMATION_MODEL_REASONED,
                    fdkId,
                    graph,
                    timestamp,
                )

            CatalogType.PUBLICSERVICES -> ServiceEvent(ServiceEventType.SERVICE_REASONED, fdkId, graph, timestamp)
            CatalogType.EVENTS -> EventEvent(EventEventType.EVENT_REASONED, fdkId, graph, timestamp)
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
