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
import java.time.Instant
import kotlin.time.measureTimedValue
import kotlin.time.toJavaDuration

@Component
open class KafkaHarvestedEventCircuitBreaker(
    private val kafkaReasonedEventProducer: KafkaReasonedEventProducer,
    private val reasoningService: ReasoningService,
    private val kafkaHarvestEventProducer: KafkaHarvestEventProducer,
) {
    @CircuitBreaker(name = CIRCUIT_BREAKER_ID)
    open fun process(record: ConsumerRecord<String, SpecificRecord>) {
        LOGGER.debug("Received message - offset: {}", record.offset())
        val event = record.value()
        val eventData = getKafkaEventData(event)

        val startTime = Instant.now()

        try {
            eventData?.let { data ->
                reasonAndProduceEvent(data, startTime)
            }
        } catch (e: Exception) {
            LOGGER.error("Error occurred during reasoning", e)
            val endTime = Instant.now()
            eventData?.let { data ->
                if (data.harvestRunId != null) {
                    kafkaHarvestEventProducer.sendReasoningFailureEvent(
                        harvestRunId = data.harvestRunId,
                        catalogType = data.resourceType,
                        fdkId = data.fdkId,
                        resourceUri = data.uri,
                        eventTimestamp = data.timestamp,
                        startTime = startTime,
                        endTime = endTime,
                        errorMessage = e.message ?: "Unknown error during reasoning",
                    )
                }
            }
            val resourceType = eventData?.resourceType
            Metrics.counter(
                "reasoning_error",
                "type",
                resourceType.toString().lowercase(),
            ).increment()
            throw e
        }
    }

    fun getKafkaEventData(event: SpecificRecord): EventData? {
        val harvestRunId = extractHarvestRunId(event)
        val uri = extractUri(event)
        return when {
            event is DatasetEvent && event.type == DatasetEventType.DATASET_HARVESTED ->
                EventData(
                    event.fdkId.toString(),
                    uri = uri,
                    event.graph.toString(),
                    event.timestamp,
                    CatalogType.DATASETS,
                    harvestRunId,
                )

            event is DatasetEvent -> null

            event is ConceptEvent && event.type == ConceptEventType.CONCEPT_HARVESTED ->
                EventData(
                    event.fdkId.toString(),
                    uri = uri,
                    event.graph.toString(),
                    event.timestamp,
                    CatalogType.CONCEPTS,
                    harvestRunId,
                )

            event is ConceptEvent -> null

            event is DataServiceEvent && event.type == DataServiceEventType.DATA_SERVICE_HARVESTED ->
                EventData(
                    event.fdkId.toString(),
                    uri = uri,
                    event.graph.toString(),
                    event.timestamp,
                    CatalogType.DATASERVICES,
                    harvestRunId,
                )

            event is DataServiceEvent -> null

            event is InformationModelEvent && event.type == InformationModelEventType.INFORMATION_MODEL_HARVESTED ->
                EventData(
                    event.fdkId.toString(),
                    uri = uri,
                    event.graph.toString(),
                    event.timestamp,
                    CatalogType.INFORMATIONMODELS,
                    harvestRunId,
                )

            event is InformationModelEvent -> null

            event is ServiceEvent && event.type == ServiceEventType.SERVICE_HARVESTED ->
                EventData(
                    event.fdkId.toString(),
                    uri = uri,
                    event.graph.toString(),
                    event.timestamp,
                    CatalogType.PUBLICSERVICES,
                    harvestRunId,
                )

            event is ServiceEvent -> null

            event is EventEvent && event.type == EventEventType.EVENT_HARVESTED ->
                EventData(
                    event.fdkId.toString(),
                    uri = uri,
                    event.graph.toString(),
                    event.timestamp,
                    CatalogType.EVENTS,
                    harvestRunId,
                )

            event is EventEvent -> null

            else -> {
                LOGGER.warn("Unrecognized event was ignored: {}", event.toString())
                null
            }
        }
    }

    private fun extractHarvestRunId(event: SpecificRecord): String? {
        return try {
            val schema = event.schema
            val harvestRunIdField = schema.getField("harvestRunId")
            if (harvestRunIdField != null) {
                val fieldIndex = harvestRunIdField.pos()
                val value = event.get(fieldIndex)
                value?.toString()
            } else {
                null
            }
        } catch (e: Exception) {
            LOGGER.debug("Could not extract harvestRunId from event: {}", e.message)
            null
        }
    }

    private fun extractUri(event: SpecificRecord): String? {
        return try {
            val schema = event.schema
            val uriField = schema.getField("uri")
            if (uriField != null) {
                val fieldIndex = uriField.pos()
                val value = event.get(fieldIndex)
                value?.toString()
            } else {
                null
            }
        } catch (e: Exception) {
            LOGGER.debug("Could not extract uri from event: {}", e.message)
            null
        }
    }

    private fun reasonAndProduceEvent(
        eventData: EventData,
        startTime: Instant,
    ) {
        LOGGER.debug("Reason {} - id: {}", eventData.resourceType, eventData.fdkId)
        val timeElapsed =
            measureTimedValue {
                reasoningService.reasonGraph(eventData.graph, eventData.resourceType)
            }
        val reasonedGraph = timeElapsed.value
        val endTime = Instant.now()

        if (reasonedGraph.isNotEmpty()) {
            Metrics.timer(
                "reasoning",
                "type",
                eventData.resourceType.toString().lowercase(),
            ).record(timeElapsed.duration.toJavaDuration())
            kafkaReasonedEventProducer.sendMessage(
                eventData.fdkId,
                reasonedGraph,
                eventData.timestamp,
                eventData.resourceType,
            )
            if (eventData.harvestRunId != null) {
                kafkaHarvestEventProducer.sendReasoningSuccessEvent(
                    harvestRunId = eventData.harvestRunId,
                    catalogType = eventData.resourceType,
                    fdkId = eventData.fdkId,
                    resourceUri = eventData.uri,
                    eventTimestamp = eventData.timestamp,
                    startTime = startTime,
                    endTime = endTime,
                )
            }
        } else {
            throw Exception("Reasoned graph is empty")
        }
    }

    data class EventData(
        val fdkId: String,
        val uri: String? = null,
        val graph: String,
        val timestamp: Long,
        val resourceType: CatalogType,
        val harvestRunId: String?,
    )

    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(KafkaHarvestedEventCircuitBreaker::class.java)
        const val CIRCUIT_BREAKER_ID = "reasoning-cb"
    }
}
