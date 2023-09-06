package no.fdk.fdk_reasoning_service.integration

import no.fdk.fdk_reasoning_service.service.EventService
import no.fdk.fdk_reasoning_service.utils.ApiTestContext
import no.fdk.fdk_reasoning_service.utils.EVENT_CATALOG_ID
import no.fdk.fdk_reasoning_service.utils.EVENT_ID_0
import no.fdk.fdk_reasoning_service.utils.EVENT_REPORT
import no.fdk.fdk_reasoning_service.utils.RDF_DATA
import no.fdk.fdk_reasoning_service.utils.TEST_DATE
import no.fdk.fdk_reasoning_service.utils.TestResponseReader
import no.fdk.fdk_reasoning_service.utils.apiGet
import org.apache.jena.riot.Lang
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.platform.suite.api.Suite
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.test.context.ContextConfiguration
import kotlin.test.assertTrue

@Suite
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(
    properties = ["spring.profiles.active=integration-test"],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = [ApiTestContext.Initializer::class])
@Tag("integration")
class EventsIntegration : ApiTestContext() {
    private val responseReader = TestResponseReader()

    @Autowired
    private lateinit var eventService: EventService

    @BeforeAll
    fun runReasoning() {
        eventService.reasonReportedChanges(EVENT_REPORT, RDF_DATA, TEST_DATE)
        eventService.updateUnion()
    }

    @Test
    fun idDoesNotExist() {
        val response = apiGet(port, "/events/123", "text/turtle")
        assertEquals(HttpStatus.NOT_FOUND.value(), response["status"])
    }

    @Test
    fun findSpecificEvent() {
        val response = apiGet(port, "/events/$EVENT_ID_0", "application/rdf+json")
        assumeTrue(HttpStatus.OK.value() == response["status"])

        val expected = responseReader.parseTurtleFile("reasoned_event_0.ttl")
        val responseModel = responseReader.parseResponse(response["body"] as String, "RDF/JSON")

        assertTrue(expected.isIsomorphicWith(responseModel))
    }

    @Test
    fun findSpecificCatalog() {
        val response = apiGet(port, "/events/catalogs/$EVENT_CATALOG_ID", "application/rdf+json")
        assumeTrue(HttpStatus.OK.value() == response["status"])

        val expected = responseReader.parseTurtleFile("reasoned_event_catalogs.ttl")
        val responseModel = responseReader.parseResponse(response["body"] as String, "RDF/JSON")

        assertTrue(expected.isIsomorphicWith(responseModel))
    }

    @Test
    fun findAllEvents() {
        val response = apiGet(port, "/events", "text/turtle")
        assumeTrue(HttpStatus.OK.value() == response["status"])

        val expected = responseReader.parseTurtleFile("reasoned_event_catalogs.ttl")
        val responseModel = responseReader.parseResponse(response["body"] as String, Lang.TURTLE.name)

        assertTrue(expected.isIsomorphicWith(responseModel))
    }

    @Test
    fun findAllCatalogs() {
        val response = apiGet(port, "/events/catalogs", "text/turtle")
        assumeTrue(HttpStatus.OK.value() == response["status"])

        val expected = responseReader.parseTurtleFile("reasoned_event_catalogs.ttl")
        val responseModel = responseReader.parseResponse(response["body"] as String, Lang.TURTLE.name)

        assertTrue(expected.isIsomorphicWith(responseModel))
    }

}
