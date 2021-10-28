package no.fdk.fdk_reasoning_service.unit

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import no.fdk.fdk_reasoning_service.config.ApplicationURI
import no.fdk.fdk_reasoning_service.model.CatalogType
import no.fdk.fdk_reasoning_service.service.ReasoningService
import no.fdk.fdk_reasoning_service.utils.ApiTestContext
import no.fdk.fdk_reasoning_service.utils.LOCAL_SERVER_PORT
import no.fdk.fdk_reasoning_service.utils.TestResponseReader
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

@Tag("unit")
class Reasoning: ApiTestContext() {
    private val uris: ApplicationURI = mock()
    private val reasoningService = ReasoningService(uris)

    private val responseReader = TestResponseReader()

    @Test
    fun testDatasets() {
        whenever(uris.datasets)
            .thenReturn("http://localhost:$LOCAL_SERVER_PORT/datasets/catalogs")
        whenever(uris.organizations)
            .thenReturn("http://localhost:$LOCAL_SERVER_PORT/organizations")
        val result = reasoningService.catalogReasoning(CatalogType.DATASETS)

        val expected = responseReader.parseTurtleFile("fdk_ready_datasets.ttl")

        assertTrue(result.isIsomorphicWith(expected))
    }

    @Test
    fun testDataServices() {
        whenever(uris.dataservices)
            .thenReturn("http://localhost:$LOCAL_SERVER_PORT/dataservices/catalogs")
        whenever(uris.organizations)
            .thenReturn("http://localhost:$LOCAL_SERVER_PORT/organizations")
        val result = reasoningService.catalogReasoning(CatalogType.DATASERVICES)

        val expected = responseReader.parseTurtleFile("fdk_ready_dataservices.ttl")

        assertTrue(result.isIsomorphicWith(expected))
    }

    @Test
    fun testConcepts() {
        whenever(uris.concepts)
            .thenReturn("http://localhost:$LOCAL_SERVER_PORT/concepts")
        whenever(uris.organizations)
            .thenReturn("http://localhost:$LOCAL_SERVER_PORT/organizations")
        val result = reasoningService.catalogReasoning(CatalogType.CONCEPTS)

        val expected = responseReader.parseTurtleFile("fdk_ready_concepts.ttl")

        assertTrue(result.isIsomorphicWith(expected))
    }

    @Test
    fun testInformationModels() {
        whenever(uris.informationmodels)
            .thenReturn("http://localhost:$LOCAL_SERVER_PORT/informationmodels/catalogs")
        whenever(uris.organizations)
            .thenReturn("http://localhost:$LOCAL_SERVER_PORT/organizations")
        val result = reasoningService.catalogReasoning(CatalogType.INFORMATIONMODELS)

        val expected = responseReader.parseTurtleFile("fdk_ready_infomodels.ttl")

        assertTrue(result.isIsomorphicWith(expected))
    }

    @Test
    fun testEvents() {
        whenever(uris.events)
            .thenReturn("http://localhost:$LOCAL_SERVER_PORT/events")
        whenever(uris.organizations)
            .thenReturn("http://localhost:$LOCAL_SERVER_PORT/organizations")
        val result = reasoningService.catalogReasoning(CatalogType.EVENTS)

        val expected = responseReader.parseTurtleFile("fdk_ready_events.ttl")

        assertTrue(result.isIsomorphicWith(expected))
    }

    @Test
    fun testPublicServices() {
        whenever(uris.publicservices)
            .thenReturn("http://localhost:$LOCAL_SERVER_PORT/public-services")
        whenever(uris.organizations)
            .thenReturn("http://localhost:$LOCAL_SERVER_PORT/organizations")
        val result = reasoningService.catalogReasoning(CatalogType.PUBLICSERVICES)

        val expected = responseReader.parseTurtleFile("fdk_ready_public_services.ttl")

        assertTrue(result.isIsomorphicWith(expected))
    }
}
