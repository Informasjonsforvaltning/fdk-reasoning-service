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
import org.apache.avro.generic.GenericRecord
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
    open fun process(record: ConsumerRecord<String, Any?>) {
        val event = record.value() ?: return
        LOGGER.debug("Received message - offset: {}", record.offset())
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
                        startTime = startTime,
                        endTime = endTime,
                        errorMessage = e.message ?: "Unknown error during reasoning",
                    )
                }
            }
            Metrics.counter(
                "reasoning_error",
                "type",
                (eventData?.resourceType?.toString() ?: "unknown").lowercase(),
            ).increment()
            throw e
        }
    }

    fun getKafkaEventData(event: Any): EventData? {
        return when (event) {
            is GenericRecord -> getEventDataFromGenericRecord(event)
            is SpecificRecord -> getEventDataFromSpecificRecord(event)
            else -> {
                LOGGER.warn("Unsupported event type: {}", event.javaClass.name)
                null
            }
        }
    }

    private fun getEventDataFromGenericRecord(value: GenericRecord): EventData? {
        val typeStr = value.get("type")?.toString() ?: return null
        if (!typeStr.endsWith("_HARVESTED")) return null
        val fdkId = value.get("fdkId")?.toString()
        val graph = value.get("graph")?.toString()
        val timestamp = value.get("timestamp") as? Long
        if (!hasRequiredFields(fdkId, graph, timestamp)) {
            LOGGER.debug("Ignoring message: required fields (fdkId, graph, timestamp) not set")
            return null
        }
        val harvestRunId = value.get("harvestRunId")?.toString()
        val uri = value.get("uri")?.toString()
        val catalogType = when (value.schema.fullName) {
            "no.fdk.dataset.DatasetEvent" -> CatalogType.DATASETS
            "no.fdk.concept.ConceptEvent" -> CatalogType.CONCEPTS
            "no.fdk.dataservice.DataServiceEvent" -> CatalogType.DATASERVICES
            "no.fdk.informationmodel.InformationModelEvent" -> CatalogType.INFORMATIONMODELS
            "no.fdk.service.ServiceEvent" -> CatalogType.PUBLICSERVICES
            "no.fdk.event.EventEvent" -> CatalogType.EVENTS
            else -> {
                LOGGER.debug("Ignoring GenericRecord with schema: {}", value.schema.fullName)
                return null
            }
        }
        return EventData(fdkId!!, uri, graph!!, timestamp!!, catalogType, harvestRunId)
    }

    private fun hasRequiredFields(fdkId: CharSequence?, graph: CharSequence?, timestamp: Long?): Boolean {
        return !fdkId.isNullOrBlank() && !graph.isNullOrBlank() && timestamp != null
    }

    private fun getEventDataFromSpecificRecord(event: SpecificRecord): EventData? {
        val harvestRunId = extractHarvestRunId(event)
        val uri = extractUri(event)
        return when {
            event is DatasetEvent && event.type == DatasetEventType.DATASET_HARVESTED ->
                eventDataIfRequiredFieldsSet(
                    event.fdkId?.toString(),
                    event.graph?.toString(),
                    event.timestamp,
                    CatalogType.DATASETS,
                    harvestRunId,
                    uri,
                )

            event is DatasetEvent -> null

            event is ConceptEvent && event.type == ConceptEventType.CONCEPT_HARVESTED ->
                eventDataIfRequiredFieldsSet(
                    event.fdkId?.toString(),
                    event.graph?.toString(),
                    event.timestamp,
                    CatalogType.CONCEPTS,
                    harvestRunId,
                    uri,
                )

            event is ConceptEvent -> null

            event is DataServiceEvent && event.type == DataServiceEventType.DATA_SERVICE_HARVESTED ->
                eventDataIfRequiredFieldsSet(
                    event.fdkId?.toString(),
                    event.graph?.toString(),
                    event.timestamp,
                    CatalogType.DATASERVICES,
                    harvestRunId,
                    uri,
                )

            event is DataServiceEvent -> null

            event is InformationModelEvent && event.type == InformationModelEventType.INFORMATION_MODEL_HARVESTED ->
                eventDataIfRequiredFieldsSet(
                    event.fdkId?.toString(),
                    event.graph?.toString(),
                    event.timestamp,
                    CatalogType.INFORMATIONMODELS,
                    harvestRunId,
                    uri,
                )

            event is InformationModelEvent -> null

            event is ServiceEvent && event.type == ServiceEventType.SERVICE_HARVESTED ->
                eventDataIfRequiredFieldsSet(
                    event.fdkId?.toString(),
                    event.graph?.toString(),
                    event.timestamp,
                    CatalogType.PUBLICSERVICES,
                    harvestRunId,
                    uri,
                )

            event is ServiceEvent -> null

            event is EventEvent && event.type == EventEventType.EVENT_HARVESTED ->
                eventDataIfRequiredFieldsSet(
                    event.fdkId?.toString(),
                    event.graph?.toString(),
                    event.timestamp,
                    CatalogType.EVENTS,
                    harvestRunId,
                    uri,
                )

            event is EventEvent -> null

            else -> {
                LOGGER.warn("Unrecognized SpecificRecord was ignored: {}", event.toString())
                null
            }
        }
    }

    private fun eventDataIfRequiredFieldsSet(
        fdkId: String?,
        graph: String?,
        timestamp: Long?,
        resourceType: CatalogType,
        harvestRunId: String?,
        uri: String?,
    ): EventData? {
        if (!hasRequiredFields(fdkId, graph, timestamp)) {
            LOGGER.debug("Ignoring message: required fields (fdkId, graph, timestamp) not set")
            return null
        }
        return EventData(fdkId!!, uri, graph!!, timestamp!!, resourceType, harvestRunId)
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
                eventData.harvestRunId,
                eventData.uri,
            )
            if (eventData.harvestRunId != null) {
                kafkaHarvestEventProducer.sendReasoningSuccessEvent(
                    harvestRunId = eventData.harvestRunId,
                    catalogType = eventData.resourceType,
                    fdkId = eventData.fdkId,
                    resourceUri = eventData.uri,
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
