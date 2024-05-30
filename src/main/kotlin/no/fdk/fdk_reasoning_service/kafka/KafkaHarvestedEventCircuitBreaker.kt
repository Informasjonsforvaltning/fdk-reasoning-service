package no.fdk.fdk_reasoning_service.kafka

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.micrometer.core.instrument.Metrics
import no.fdk.concept.ConceptEvent
import no.fdk.concept.ConceptEventType
import no.fdk.dataservice.DataServiceEvent
import no.fdk.dataservice.DataServiceEventType
import no.fdk.dataset.DatasetEvent
import no.fdk.dataset.DatasetEventType
import no.fdk.event.EventEvent
import no.fdk.event.EventEventType
import no.fdk.fdk_reasoning_service.model.CatalogType
import no.fdk.fdk_reasoning_service.service.ReasoningService
import no.fdk.informationmodel.InformationModelEvent
import no.fdk.informationmodel.InformationModelEventType
import no.fdk.service.ServiceEvent
import no.fdk.service.ServiceEventType
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import kotlin.time.measureTimedValue
import kotlin.time.toJavaDuration

@Component
open class KafkaHarvestedEventCircuitBreaker(
    private val kafkaReasonedEventProducer: KafkaReasonedEventProducer,
    private val reasoningService: ReasoningService,
) {
    @CircuitBreaker(name = CIRCUIT_BREAKER_ID)
    open fun process(record: ConsumerRecord<String, SpecificRecord>) {
        LOGGER.debug("Received message - offset: {}", record.offset())
        val event = record.value()
        val eventData = getKafkaEventData(event)

        try {
            eventData?.let { (fdkId, graph, timestamp, resourceType) ->
                reasonAndProduceEvent(fdkId, graph, timestamp, resourceType)
            }
        } catch (e: Exception) {
            LOGGER.error("Error occurred during reasoning", e)
            val resourceType = eventData?.resourceType
            Metrics.counter(
                "reasoning_error",
                "type",
                resourceType.toString().lowercase(),
            ).increment()
            throw e
        }
    }

    fun getKafkaEventData(event: SpecificRecord): EventData? =
        when {
            event is DatasetEvent && event.type == DatasetEventType.DATASET_HARVESTED ->
                EventData(event.fdkId.toString(), event.graph.toString(), event.timestamp, CatalogType.DATASETS)

            event is ConceptEvent && event.type == ConceptEventType.CONCEPT_HARVESTED ->
                EventData(event.fdkId.toString(), event.graph.toString(), event.timestamp, CatalogType.CONCEPTS)

            event is DataServiceEvent && event.type == DataServiceEventType.DATA_SERVICE_HARVESTED ->
                EventData(event.fdkId.toString(), event.graph.toString(), event.timestamp, CatalogType.DATASERVICES)

            event is InformationModelEvent && event.type == InformationModelEventType.INFORMATION_MODEL_HARVESTED ->
                EventData(
                    event.fdkId.toString(),
                    event.graph.toString(),
                    event.timestamp,
                    CatalogType.INFORMATIONMODELS,
                )

            event is ServiceEvent && event.type == ServiceEventType.SERVICE_HARVESTED ->
                EventData(event.fdkId.toString(), event.graph.toString(), event.timestamp, CatalogType.PUBLICSERVICES)

            event is EventEvent && event.type == EventEventType.EVENT_HARVESTED ->
                EventData(event.fdkId.toString(), event.graph.toString(), event.timestamp, CatalogType.EVENTS)

            else -> null
        }

    private fun reasonAndProduceEvent(
        fdkId: String,
        graph: String,
        timestamp: Long,
        resourceType: CatalogType,
    ) {
        LOGGER.debug("Reason {} - id: {}", resourceType, fdkId)
        val timeElapsed =
            measureTimedValue {
                reasoningService.reasonGraph(graph, resourceType)
            }
        val reasonedGraph = timeElapsed.value
        if (reasonedGraph.isNotEmpty()) {
            Metrics.timer(
                "reasoning",
                "type",
                resourceType.toString().lowercase(),
            ).record(timeElapsed.duration.toJavaDuration())
            kafkaReasonedEventProducer.sendMessage(fdkId, reasonedGraph, timestamp, resourceType)
        } else {
            throw Exception("Reasoned graph is empty")
        }
    }

    data class EventData(
        val fdkId: String,
        val graph: String,
        val timestamp: Long,
        val resourceType: CatalogType,
    )

    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(KafkaHarvestedEventCircuitBreaker::class.java)
        const val CIRCUIT_BREAKER_ID = "reasoning-cb"
    }
}
