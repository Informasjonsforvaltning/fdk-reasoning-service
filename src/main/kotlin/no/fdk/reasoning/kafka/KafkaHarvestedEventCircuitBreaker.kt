package no.fdk.reasoning.kafka

import io.github.resilience4j.circuitbreaker.CircuitBreaker
import no.fdk.concept.ConceptEvent
import no.fdk.concept.ConceptEventType
import no.fdk.dataservice.DataServiceEvent
import no.fdk.dataservice.DataServiceEventType
import no.fdk.dataset.DatasetEvent
import no.fdk.dataset.DatasetEventType
import no.fdk.event.EventEvent
import no.fdk.event.EventEventType
import no.fdk.informationmodel.InformationModelEvent
import no.fdk.informationmodel.InformationModelEventType
import no.fdk.reasoning.metrics.ReasoningMetrics
import no.fdk.reasoning.model.CatalogType
import no.fdk.reasoning.service.ReasoningService
import no.fdk.service.ServiceEvent
import no.fdk.service.ServiceEventType
import org.apache.avro.generic.GenericRecord
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import java.time.Instant
import kotlin.time.measureTimedValue

@Component
class KafkaHarvestedEventCircuitBreaker(
    private val kafkaReasonedEventProducer: KafkaReasonedEventProducer,
    private val reasoningService: ReasoningService,
    private val kafkaHarvestEventProducer: KafkaHarvestEventProducer,
    @param:Qualifier("reasoningCircuitBreaker")
    private val circuitBreaker: CircuitBreaker,
) {
    fun process(record: ConsumerRecord<String, Any?>): ProcessOutcome = circuitBreaker.executeSupplier {
        val event = record.value() ?: return@executeSupplier ProcessOutcome.Skipped
        LOGGER.debug("Received message - topic: {} partition: {} offset: {}", record.topic(), record.partition(), record.offset())
        val eventData = getKafkaEventData(event)
        if (eventData == null) {
            LOGGER.debug(
                "Ignoring message (wrong type, missing required fields, or unknown schema) - topic: {} partition: {} offset: {}",
                record.topic(),
                record.partition(),
                record.offset(),
            )
            return@executeSupplier ProcessOutcome.Skipped
        }

        val startTime = Instant.now()

        try {
            reasonAndProduceEvent(eventData, startTime)
            ProcessOutcome.Success(eventData.resourceType)
        } catch (e: Exception) {
            LOGGER.error("Error occurred during reasoning (fdkId={})", eventData.fdkId, e)
            val endTime = Instant.now()
            if (eventData.harvestRunId != null) {
                kafkaHarvestEventProducer.sendReasoningFailureEvent(
                    harvestRunId = eventData.harvestRunId,
                    catalogType = eventData.resourceType,
                    fdkId = eventData.fdkId,
                    resourceUri = eventData.uri,
                    startTime = startTime,
                    endTime = endTime,
                    errorMessage = e.message ?: "Unknown error during reasoning",
                )
            }
            ReasoningMetrics.recordError(eventData.resourceType)
            throw ReasoningProcessingException(eventData.resourceType, e)
        }
    }

    fun getKafkaEventData(event: Any): EventData? = when (event) {
        is GenericRecord -> {
            getEventDataFromGenericRecord(event)
        }

        is SpecificRecord -> {
            getEventDataFromSpecificRecord(event)
        }

        else -> {
            LOGGER.warn("Unsupported event type: {}", event.javaClass.name)
            null
        }
    }

    private fun getEventDataFromGenericRecord(value: GenericRecord): EventData? {
        val typeStr = safeGet(value, "type")?.toString() ?: return null
        if (!typeStr.endsWith("_HARVESTED")) return null
        val fdkId = safeGet(value, "fdkId")?.toString()
        val graph = safeGet(value, "graph")?.toString()
        val timestamp = safeGet(value, "timestamp") as? Long
        val catalogGraph = safeGet(value, "catalogGraph")?.toString()
        if (!hasRequiredFields(fdkId, graph, timestamp)) {
            LOGGER.debug("Ignoring message: required fields (fdkId, graph, timestamp) not set")
            return null
        }
        val harvestRunId = safeGet(value, "harvestRunId")?.toString()
        val uri = safeGet(value, "uri")?.toString()
        val catalogType =
            when (value.schema.fullName) {
                "no.fdk.dataset.DatasetEvent" -> {
                    CatalogType.DATASETS
                }

                "no.fdk.concept.ConceptEvent" -> {
                    CatalogType.CONCEPTS
                }

                "no.fdk.dataservice.DataServiceEvent" -> {
                    CatalogType.DATASERVICES
                }

                "no.fdk.informationmodel.InformationModelEvent" -> {
                    CatalogType.INFORMATIONMODELS
                }

                "no.fdk.service.ServiceEvent" -> {
                    CatalogType.PUBLICSERVICES
                }

                "no.fdk.event.EventEvent" -> {
                    CatalogType.EVENTS
                }

                else -> {
                    LOGGER.debug("Ignoring GenericRecord with schema: {}", value.schema.fullName)
                    return null
                }
            }
        return EventData(fdkId!!, uri, graph!!, timestamp!!, catalogType, harvestRunId, catalogGraph)
    }

    private fun hasRequiredFields(fdkId: CharSequence?, graph: CharSequence?, timestamp: Long?): Boolean =
        !fdkId.isNullOrBlank() && !graph.isNullOrBlank() && timestamp != null

    private fun safeGet(record: GenericRecord, fieldName: String): Any? = try {
        record.get(fieldName)
    } catch (e: Exception) {
        LOGGER.debug("Could not read field '{}' from GenericRecord: {}", fieldName, e.message)
        null
    }

    private fun getEventDataFromSpecificRecord(event: SpecificRecord): EventData? {
        val harvestRunId = extractHarvestRunId(event)
        val uri = extractUri(event)
        return when {
            event is DatasetEvent && event.type == DatasetEventType.DATASET_HARVESTED -> {
                eventDataIfRequiredFieldsSet(
                    event.fdkId?.toString(),
                    event.graph?.toString(),
                    event.timestamp,
                    CatalogType.DATASETS,
                    harvestRunId,
                    uri,
                    event.catalogGraph?.toString(),
                )
            }

            event is DatasetEvent -> {
                null
            }

            event is ConceptEvent && event.type == ConceptEventType.CONCEPT_HARVESTED -> {
                eventDataIfRequiredFieldsSet(
                    event.fdkId?.toString(),
                    event.graph?.toString(),
                    event.timestamp,
                    CatalogType.CONCEPTS,
                    harvestRunId,
                    uri,
                    event.catalogGraph?.toString(),
                )
            }

            event is ConceptEvent -> {
                null
            }

            event is DataServiceEvent && event.type == DataServiceEventType.DATA_SERVICE_HARVESTED -> {
                eventDataIfRequiredFieldsSet(
                    event.fdkId?.toString(),
                    event.graph?.toString(),
                    event.timestamp,
                    CatalogType.DATASERVICES,
                    harvestRunId,
                    uri,
                    event.catalogGraph?.toString(),
                )
            }

            event is DataServiceEvent -> {
                null
            }

            event is InformationModelEvent && event.type == InformationModelEventType.INFORMATION_MODEL_HARVESTED -> {
                eventDataIfRequiredFieldsSet(
                    event.fdkId?.toString(),
                    event.graph?.toString(),
                    event.timestamp,
                    CatalogType.INFORMATIONMODELS,
                    harvestRunId,
                    uri,
                    event.catalogGraph?.toString(),
                )
            }

            event is InformationModelEvent -> {
                null
            }

            event is ServiceEvent && event.type == ServiceEventType.SERVICE_HARVESTED -> {
                eventDataIfRequiredFieldsSet(
                    event.fdkId?.toString(),
                    event.graph?.toString(),
                    event.timestamp,
                    CatalogType.PUBLICSERVICES,
                    harvestRunId,
                    uri,
                    event.catalogGraph?.toString(),
                )
            }

            event is ServiceEvent -> {
                null
            }

            event is EventEvent && event.type == EventEventType.EVENT_HARVESTED -> {
                eventDataIfRequiredFieldsSet(
                    event.fdkId?.toString(),
                    event.graph?.toString(),
                    event.timestamp,
                    CatalogType.EVENTS,
                    harvestRunId,
                    uri,
                    event.catalogGraph?.toString(),
                )
            }

            event is EventEvent -> {
                null
            }

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
        catalogGraph: String?,
    ): EventData? {
        if (!hasRequiredFields(fdkId, graph, timestamp)) {
            LOGGER.debug("Ignoring message: required fields (fdkId, graph, timestamp) not set")
            return null
        }
        return EventData(fdkId!!, uri, graph!!, timestamp!!, resourceType, harvestRunId, catalogGraph)
    }

    private fun extractHarvestRunId(event: SpecificRecord): String? = try {
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

    private fun extractUri(event: SpecificRecord): String? = try {
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

    private fun reasonAndProduceEvent(eventData: EventData, startTime: Instant) {
        LOGGER.debug("Reasoning {} - id: {}", eventData.resourceType, eventData.fdkId)
        val timeElapsed =
            measureTimedValue {
                reasoningService.reasonGraph(eventData.graph, eventData.resourceType, eventData.catalogGraph)
            }
        val reasonedGraph = timeElapsed.value
        val endTime = Instant.now()

        if (reasonedGraph.isNotEmpty()) {
            ReasoningMetrics.recordTotal(eventData.resourceType, timeElapsed.duration)
            val sent =
                kafkaReasonedEventProducer.sendMessage(
                    eventData.fdkId,
                    reasonedGraph,
                    eventData.timestamp,
                    eventData.resourceType,
                    eventData.catalogGraph,
                    eventData.harvestRunId,
                    eventData.uri,
                )
            if (sent && eventData.harvestRunId != null) {
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

    sealed class ProcessOutcome {
        data object Skipped : ProcessOutcome()

        data class Success(val catalogType: CatalogType) : ProcessOutcome()
    }

    data class EventData(
        val fdkId: String,
        val uri: String? = null,
        val graph: String,
        val timestamp: Long,
        val resourceType: CatalogType,
        val harvestRunId: String?,
        val catalogGraph: String?,
    )

    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(KafkaHarvestedEventCircuitBreaker::class.java)
    }
}
