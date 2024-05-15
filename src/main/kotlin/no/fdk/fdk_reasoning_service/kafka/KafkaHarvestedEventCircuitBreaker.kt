package no.fdk.fdk_reasoning_service.kafka

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
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
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class KafkaHarvestedEventCircuitBreaker {
    @CircuitBreaker(name = "reasoning")
    fun process(record: ConsumerRecord<String, SpecificRecord>) {
        LOGGER.debug("Received message - offset: ${record.offset()}")
        val event = record.value()

        try {
            getKafkaEventData(event)?.let {
                val (fdkId, graph, timestamp, resourceType) = it
                LOGGER.debug("Reason {} - id: {}", resourceType, fdkId)
                reasonAndProduceEvent(fdkId, graph, timestamp, resourceType)
            }

        } catch (e: Exception) {
            LOGGER.error("Error occurred during reasoning: " + e.message)
            throw e
        }
    }

    private fun getKafkaEventData(event: SpecificRecord): EventData? {
        event.let {
            if (it is DatasetEvent && it.type == DatasetEventType.DATASET_HARVESTED) {
                return EventData(it.fdkId.toString(), it.graph.toString(), it.timestamp, CatalogType.DATASETS)
            } else if (it is ConceptEvent && it.type == ConceptEventType.CONCEPT_HARVESTED) {
                return EventData(it.fdkId.toString(), it.graph.toString(), it.timestamp, CatalogType.CONCEPTS)
            } else if (it is DataServiceEvent && it.type == DataServiceEventType.DATA_SERVICE_HARVESTED) {
                return EventData(it.fdkId.toString(), it.graph.toString(), it.timestamp, CatalogType.DATASERVICES)
            } else if (it is InformationModelEvent && it.type == InformationModelEventType.INFORMATION_MODEL_HARVESTED) {
                return EventData(it.fdkId.toString(), it.graph.toString(), it.timestamp, CatalogType.INFORMATIONMODELS)
            } else if (it is ServiceEvent && it.type == ServiceEventType.SERVICE_HARVESTED) {
                return EventData(it.fdkId.toString(), it.graph.toString(), it.timestamp, CatalogType.PUBLICSERVICES)
            } else if (it is EventEvent && it.type == EventEventType.EVENT_HARVESTED) {
                return EventData(it.fdkId.toString(), it.graph.toString(), it.timestamp, CatalogType.EVENTS)
            } else {
                return null
            }
        }
    }

    private fun reasonAndProduceEvent(fdkId: String, graph: String, timestamp: Long, resourceType: CatalogType) {
        LOGGER.debug("Reason dataset - id: $fdkId")
        // TODO: reason on graph
        // TODO: produce kafka message with producer
    }

    private data class EventData(val fdkId: String, val graph: String, val timestamp: Long, val resourceType: CatalogType)

    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(KafkaHarvestedEventCircuitBreaker::class.java)
    }
}
