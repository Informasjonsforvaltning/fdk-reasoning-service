package no.fdk.fdk_reasoning_service.unit

import io.mockk.mockk
import no.fdk.concept.ConceptEvent
import no.fdk.concept.ConceptEventType
import no.fdk.dataservice.DataServiceEvent
import no.fdk.dataservice.DataServiceEventType
import no.fdk.dataset.DatasetEvent
import no.fdk.dataset.DatasetEventType
import no.fdk.event.EventEvent
import no.fdk.event.EventEventType
import no.fdk.fdk_reasoning_service.kafka.KafkaHarvestEventProducer
import no.fdk.fdk_reasoning_service.kafka.KafkaHarvestedEventCircuitBreaker
import no.fdk.fdk_reasoning_service.kafka.KafkaHarvestedEventCircuitBreaker.EventData
import no.fdk.fdk_reasoning_service.model.CatalogType
import no.fdk.informationmodel.InformationModelEvent
import no.fdk.informationmodel.InformationModelEventType
import no.fdk.service.ServiceEvent
import no.fdk.service.ServiceEventType
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

@Tag("unit")
class Kafka {
    private val circuitBreaker = KafkaHarvestedEventCircuitBreaker(mockk(), mockk(), mockk())

    @Nested
    inner class GetDataEvent {
        @Test
        fun `dataset event type returns correct event data`() {
            val input = DatasetEvent(DatasetEventType.DATASET_HARVESTED, null, null, "fdk-id", "graph", 0)
            val expected = EventData("fdk-id", uri = null, "graph", 0, CatalogType.DATASETS, harvestRunId = null)
            assertEquals(circuitBreaker.getKafkaEventData(input), expected)
        }

        @Test
        fun `concept event type returns correct event data`() {
            val input = ConceptEvent(ConceptEventType.CONCEPT_HARVESTED, null, null, "fdk-id", "graph", 0)
            val expected = EventData("fdk-id", uri = null, "graph", 0, CatalogType.CONCEPTS, harvestRunId = null)
            assertEquals(circuitBreaker.getKafkaEventData(input), expected)
        }

        @Test
        fun `data service event type returns correct event data`() {
            val input = DataServiceEvent(DataServiceEventType.DATA_SERVICE_HARVESTED, null, null, "fdk-id", "graph", 0)
            val expected = EventData("fdk-id", uri = null, "graph", 0, CatalogType.DATASERVICES, harvestRunId = null)
            assertEquals(circuitBreaker.getKafkaEventData(input), expected)
        }

        @Test
        fun `information model event type returns correct event data`() {
            val input = InformationModelEvent(InformationModelEventType.INFORMATION_MODEL_HARVESTED, null, null, "fdk-id", "graph", 0)
            val expected = EventData("fdk-id", uri = null, "graph", 0, CatalogType.INFORMATIONMODELS, harvestRunId = null)
            assertEquals(circuitBreaker.getKafkaEventData(input), expected)
        }

        @Test
        fun `service event type returns correct event data`() {
            val input = ServiceEvent(ServiceEventType.SERVICE_HARVESTED, null, null, "fdk-id", "graph", 0)
            val expected = EventData("fdk-id", uri = null, "graph", 0, CatalogType.PUBLICSERVICES, harvestRunId = null)
            assertEquals(circuitBreaker.getKafkaEventData(input), expected)
        }

        @Test
        fun `event event type returns correct event data`() {
            val input = EventEvent(EventEventType.EVENT_HARVESTED, null, null, "fdk-id", "graph", 0)
            val expected = EventData("fdk-id", uri = null, "graph", 0, CatalogType.EVENTS, harvestRunId = null)
            assertEquals(circuitBreaker.getKafkaEventData(input), expected)
        }

        @Test
        fun `removed event type returns null`() {
            val input = ConceptEvent(ConceptEventType.CONCEPT_REMOVED, null, null, "fdk-id", "graph", 0)
            assertEquals(circuitBreaker.getKafkaEventData(input), null)
        }
    }
}
