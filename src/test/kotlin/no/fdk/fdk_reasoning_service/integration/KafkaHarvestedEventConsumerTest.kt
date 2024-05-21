package no.fdk.fdk_reasoning_service.integration

import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.fdk.dataset.DatasetEvent
import no.fdk.dataset.DatasetEventType
import no.fdk.fdk_reasoning_service.kafka.KafkaHarvestedEventCircuitBreaker
import no.fdk.fdk_reasoning_service.kafka.KafkaHarvestedEventConsumer
import no.fdk.fdk_reasoning_service.kafka.KafkaReasonedEventProducer
import no.fdk.fdk_reasoning_service.model.CatalogType
import no.fdk.fdk_reasoning_service.service.ReasoningService
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.Test
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.Acknowledgment
import org.springframework.test.context.ActiveProfiles
import java.time.Duration
import java.util.concurrent.CompletableFuture
import kotlin.test.assertEquals
import kotlin.test.assertIs

@ActiveProfiles("test")
class KafkaHarvestedEventConsumerTest {
    private val reasoningService: ReasoningService = mockk()
    private val kafkaTemplate: KafkaTemplate<String, SpecificRecord> = mockk()
    private val ack: Acknowledgment = mockk()
    private val kafkaRdfParseEventProducer = KafkaReasonedEventProducer(kafkaTemplate)
    private val circuitBreaker = KafkaHarvestedEventCircuitBreaker(kafkaRdfParseEventProducer, reasoningService)
    private val kafkaHarvestedEventConsumer = KafkaHarvestedEventConsumer(circuitBreaker)

    /* Ignores checking the mocked graph returned from reasoningService,
     * since the reasoning functionality is already tested.
     */
    @Test
    fun `listen should produce a reasoned event`() {
        val inputGraph = """<http://data.test.no/catalogs/1/datasets/1> a <http://www.w3.org/ns/dcat#Dataset> ."""
        val outputGraph =
            """
                <http://data.test.no/catalogs/1/datasets/1> a <http://www.w3.org/ns/dcat#Dataset> .
                <http://data.test.no/catalogs/1/datasets/1> a <http://www.w3.org/ns/dcat#Resource> .
            """.trimMargin()
        every { reasoningService.reasonGraph(inputGraph, CatalogType.DATASETS) } returns outputGraph
        every { kafkaTemplate.send(any(), any()) } returns CompletableFuture()
        every { ack.acknowledge() } returns Unit
        every { ack.nack(Duration.ZERO) } returns Unit

        val datasetEvent =
            DatasetEvent(DatasetEventType.DATASET_HARVESTED, "my-id", inputGraph, System.currentTimeMillis())
        kafkaHarvestedEventConsumer.listen(
            record = ConsumerRecord("dataset-events", 0, 0, "my-id", datasetEvent),
            ack = ack,
        )

        verify {
            kafkaTemplate.send(
                withArg {
                    assertEquals("dataset-events", it)
                },
                withArg {
                    assertIs<DatasetEvent>(it)
                    assertEquals(datasetEvent.fdkId, it.fdkId)
                    assertEquals(datasetEvent.type, it.type)
                    assertEquals(datasetEvent.timestamp, it.timestamp)
                },
            )
            ack.acknowledge()
        }
        confirmVerified(kafkaTemplate, ack)
    }

    @Test
    fun `listen should acknowledge but not reason when a REMOVED event is received`() {
        every { ack.acknowledge() } returns Unit
        every { ack.nack(Duration.ZERO) } returns Unit

        val datasetEvent = DatasetEvent(DatasetEventType.DATASET_REMOVED, "my-id", "uri", System.currentTimeMillis())
        kafkaHarvestedEventConsumer.listen(
            record = ConsumerRecord("dataset-events", 0, 0, "my-id", datasetEvent),
            ack = ack,
        )

        verify(exactly = 0) { kafkaTemplate.send(any(), any()) }
        verify(exactly = 1) { ack.acknowledge() }
        verify(exactly = 0) { ack.nack(Duration.ZERO) }
        confirmVerified(kafkaTemplate, ack)
    }

    @Test
    fun `listen should not acknowledge when an exception occurs`() {
        every { reasoningService.reasonGraph(any(), any()) } throws Exception("Error on reasoning RDF")
        every { ack.nack(Duration.ZERO) } returns Unit

        val datasetEvent = DatasetEvent(DatasetEventType.DATASET_HARVESTED, "my-id", "uri", System.currentTimeMillis())
        kafkaHarvestedEventConsumer.listen(
            record = ConsumerRecord("dataset-events", 0, 0, "my-id", datasetEvent),
            ack = ack,
        )

        verify(exactly = 0) { kafkaTemplate.send(any(), any()) }
        verify(exactly = 0) { ack.acknowledge() }
        verify(exactly = 1) { ack.nack(Duration.ZERO) }
        confirmVerified(kafkaTemplate, ack)
    }
}
