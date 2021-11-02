package no.fdk.fdk_reasoning_service.unit

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import no.fdk.fdk_reasoning_service.config.ApplicationURI
import no.fdk.fdk_reasoning_service.model.CatalogType
import no.fdk.fdk_reasoning_service.rdf.CV
import no.fdk.fdk_reasoning_service.service.ReasoningService
import no.fdk.fdk_reasoning_service.service.catalogRecordURI
import no.fdk.fdk_reasoning_service.utils.ApiTestContext
import no.fdk.fdk_reasoning_service.utils.LOCAL_SERVER_PORT
import no.fdk.fdk_reasoning_service.utils.TestResponseReader
import org.apache.jena.rdf.model.Model
import org.apache.jena.riot.Lang
import org.apache.jena.vocabulary.RDF
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import kotlin.test.assertTrue

@Tag("unit")
class Reasoning: ApiTestContext() {
    private val uris: ApplicationURI = mock()
    private val reasoningService = ReasoningService(uris)

    private val responseReader = TestResponseReader()

    @Test
    fun testDatasets() {
        whenever(uris.organizations)
            .thenReturn("http://localhost:$LOCAL_SERVER_PORT/organizations")
        whenever(uris.los)
            .thenReturn("http://localhost:$LOCAL_SERVER_PORT/los")
        val result = reasoningService.catalogReasoning(responseReader.parseTurtleFile("datasets.ttl"), CatalogType.DATASETS)

        val expected = responseReader.parseTurtleFile("fdk_ready_datasets.ttl")

        assertTrue(result.isIsomorphicWith(expected))
    }

    @Test
    fun testDataServices() {
        whenever(uris.organizations)
            .thenReturn("http://localhost:$LOCAL_SERVER_PORT/organizations")
        whenever(uris.los)
            .thenReturn("http://localhost:$LOCAL_SERVER_PORT/los")
        val result = reasoningService.catalogReasoning(responseReader.parseTurtleFile("dataservices.ttl"), CatalogType.DATASERVICES)

        val expected = responseReader.parseTurtleFile("fdk_ready_dataservices.ttl")

        assertTrue(result.isIsomorphicWith(expected))
    }

    @Test
    fun testConcepts() {
        whenever(uris.organizations)
            .thenReturn("http://localhost:$LOCAL_SERVER_PORT/organizations")
        whenever(uris.los)
            .thenReturn("http://localhost:$LOCAL_SERVER_PORT/los")
        val result = reasoningService.catalogReasoning(responseReader.parseTurtleFile("concepts.ttl"), CatalogType.CONCEPTS)

        val expected = responseReader.parseTurtleFile("fdk_ready_concepts.ttl")

        assertTrue(result.isIsomorphicWith(expected))
    }

    @Test
    fun testInformationModels() {
        whenever(uris.organizations)
            .thenReturn("http://localhost:$LOCAL_SERVER_PORT/organizations")
        whenever(uris.los)
            .thenReturn("http://localhost:$LOCAL_SERVER_PORT/los")
        val result = reasoningService.catalogReasoning(responseReader.parseTurtleFile("infomodels.ttl"), CatalogType.INFORMATIONMODELS)

        val expected = responseReader.parseTurtleFile("fdk_ready_infomodels.ttl")

        assertTrue(result.isIsomorphicWith(expected))
    }

    @Test
    fun testEvents() {
        whenever(uris.organizations)
            .thenReturn("http://localhost:$LOCAL_SERVER_PORT/organizations")
        whenever(uris.los)
            .thenReturn("http://localhost:$LOCAL_SERVER_PORT/los")
        val result = reasoningService.catalogReasoning(responseReader.parseTurtleFile("events.ttl"), CatalogType.EVENTS)

        val expected = responseReader.parseTurtleFile("fdk_ready_events.ttl")

        assertTrue(result.isIsomorphicWith(expected))
    }

    @Test
    fun testPublicServices() {
        whenever(uris.organizations)
            .thenReturn("http://localhost:$LOCAL_SERVER_PORT/organizations")
        whenever(uris.los)
            .thenReturn("http://localhost:$LOCAL_SERVER_PORT/los")
        val result = reasoningService.catalogReasoning(responseReader.parseTurtleFile("public_services.ttl"), CatalogType.PUBLICSERVICES)

        val expected = responseReader.parseTurtleFile("fdk_ready_public_services.ttl")

        assertTrue(result.isIsomorphicWith(expected))
    }
}
