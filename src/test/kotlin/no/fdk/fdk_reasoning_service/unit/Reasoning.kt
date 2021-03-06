package no.fdk.fdk_reasoning_service.unit

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import no.fdk.fdk_reasoning_service.config.ApplicationURI
import no.fdk.fdk_reasoning_service.model.CatalogType
import no.fdk.fdk_reasoning_service.service.ReasoningService
import no.fdk.fdk_reasoning_service.utils.*
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

@Tag("unit")
class Reasoning : ApiTestContext() {
    private val uris: ApplicationURI = mock()
    private val reasoningService = ReasoningService(uris)

    private val responseReader = TestResponseReader()

    @Test
    fun testDatasets() {
        whenever(uris.organizations)
            .thenReturn("http://localhost:$LOCAL_SERVER_PORT/organizations")
        val result = reasoningService.catalogReasoning(
            responseReader.parseTurtleFile("datasets.ttl"),
            CatalogType.DATASETS,
            RDF_DATA
        )
        val expected = responseReader.parseTurtleFile("fdk_ready_datasets.ttl")

        assertTrue(result.isIsomorphicWith(expected))
    }

    @Test
    fun testDatasetSeries() {
        whenever(uris.organizations)
            .thenReturn("http://localhost:$LOCAL_SERVER_PORT/organizations")
        val result = reasoningService.catalogReasoning(
            responseReader.parseTurtleFile("dataset_series.ttl"),
            CatalogType.DATASETS,
            RDF_DATA
        )
        val expected = responseReader.parseTurtleFile("fdk_ready_dataset_series.ttl")

        assertTrue(result.isIsomorphicWith(expected))
    }

    @Test
    fun testDataServices() {
        whenever(uris.organizations)
            .thenReturn("http://localhost:$LOCAL_SERVER_PORT/organizations")
        val result = reasoningService.catalogReasoning(
            responseReader.parseTurtleFile("dataservices.ttl"),
            CatalogType.DATASERVICES,
            RDF_DATA
        )
        val expected = responseReader.parseTurtleFile("fdk_ready_dataservices.ttl")

        assertTrue(result.isIsomorphicWith(expected))
    }

    @Test
    fun testConcepts() {
        whenever(uris.organizations)
            .thenReturn("http://localhost:$LOCAL_SERVER_PORT/organizations")
        val result = reasoningService.catalogReasoning(
            responseReader.parseTurtleFile("concepts.ttl"),
            CatalogType.CONCEPTS,
            RDF_DATA
        )
        val expected = responseReader.parseTurtleFile("fdk_ready_concepts.ttl")

        assertTrue(result.isIsomorphicWith(expected))
    }

    @Test
    fun testInformationModels() {
        whenever(uris.organizations)
            .thenReturn("http://localhost:$LOCAL_SERVER_PORT/organizations")
        val result = reasoningService.catalogReasoning(
            responseReader.parseTurtleFile("infomodels.ttl"),
            CatalogType.INFORMATIONMODELS,
            RDF_DATA
        )
        val expected = responseReader.parseTurtleFile("fdk_ready_infomodels.ttl")

        assertTrue(result.isIsomorphicWith(expected))
    }

    @Test
    fun testEvents() {
        whenever(uris.organizations)
            .thenReturn("http://localhost:$LOCAL_SERVER_PORT/organizations")
        val result = reasoningService.catalogReasoning(
            responseReader.parseTurtleFile("event_0.ttl"),
            CatalogType.EVENTS,
            RDF_DATA
        )
        val expected = responseReader.parseTurtleFile("fdk_ready_event_0.ttl")

        assertTrue(result.isIsomorphicWith(expected))
    }

    @Test
    fun testPublicService() {
        whenever(uris.organizations)
            .thenReturn("http://localhost:$LOCAL_SERVER_PORT/organizations")
        val result = reasoningService.catalogReasoning(
            responseReader.parseTurtleFile("public_service_0.ttl"),
            CatalogType.PUBLICSERVICES,
            RDF_DATA
        )
        val expected = responseReader.parseTurtleFile("fdk_ready_public_service_0.ttl")

        assertTrue(result.isIsomorphicWith(expected))
    }
}
