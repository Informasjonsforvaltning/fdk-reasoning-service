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
            event.let {
                if (it is DatasetEvent && it.type == DatasetEventType.DATASET_HARVESTED) {
                    LOGGER.debug("Reason dataset - id: " + it.fdkId)
                    reasonAndProduceEvent(it.fdkId.toString(), it.graph.toString(), it.timestamp, CatalogType.DATASETS)
                } else if (it is ConceptEvent && it.type == ConceptEventType.CONCEPT_HARVESTED) {
                    LOGGER.debug("Reason concept - id: " + it.fdkId)
                    reasonAndProduceEvent(it.fdkId.toString(), it.graph.toString(), it.timestamp, CatalogType.CONCEPTS)
                } else if (it is DataServiceEvent && it.type == DataServiceEventType.DATA_SERVICE_HARVESTED) {
                    LOGGER.debug("Reason data-service - id: " + it.fdkId)
                    reasonAndProduceEvent(
                        it.fdkId.toString(),
                        it.graph.toString(),
                        it.timestamp,
                        CatalogType.INFORMATIONMODELS
                    )
                } else if (it is InformationModelEvent && it.type == InformationModelEventType.INFORMATION_MODEL_HARVESTED) {
                    LOGGER.debug("Reason information-model - id: " + it.fdkId)
                    reasonAndProduceEvent(
                        it.fdkId.toString(),
                        it.graph.toString(),
                        it.timestamp,
                        CatalogType.INFORMATIONMODELS
                    )
                } else if (it is ServiceEvent && it.type == ServiceEventType.SERVICE_HARVESTED) {
                    LOGGER.debug("Reason information-model - id: " + it.fdkId)
                    reasonAndProduceEvent(
                        it.fdkId.toString(),
                        it.graph.toString(),
                        it.timestamp,
                        CatalogType.PUBLICSERVICES
                    )
                } else if (it is EventEvent && it.type == EventEventType.EVENT_HARVESTED) {
                    LOGGER.debug("Reason information-model - id: " + it.fdkId)
                    reasonAndProduceEvent(it.fdkId.toString(), it.graph.toString(), it.timestamp, CatalogType.EVENTS)
                }
            }
        } catch (e: Exception) {
            LOGGER.error("Error occurred during reasoning: " + e.message)
            throw e
        }
    }

    private fun reasonAndProduceEvent(fdkId: String, graph: String, timestamp: Long, resourceType: CatalogType) {
        LOGGER.debug("Reason dataset - id: $fdkId")
        // TODO: reason on graph
        // TODO: produce kafka message with producer
    }


    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(KafkaHarvestedEventCircuitBreaker::class.java)
    }
}
