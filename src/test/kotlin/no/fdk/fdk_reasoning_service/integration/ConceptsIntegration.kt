package no.fdk.fdk_reasoning_service.integration

import no.fdk.fdk_reasoning_service.service.ConceptService
import no.fdk.fdk_reasoning_service.utils.*
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
class ConceptsIntegration : ApiTestContext() {
    private val responseReader = TestResponseReader()

    @Autowired
    private lateinit var conceptService: ConceptService

    @BeforeAll
    fun runReasoning() {
        conceptService.reasonReportedChanges(CONCEPT_REPORT, RDF_DATA, TEST_DATE)
        conceptService.updateUnion()
    }

    @Test
    fun idDoesNotExist() {
        val response = apiGet(port, "/concepts/123", "text/turtle")
        assertEquals(HttpStatus.NOT_FOUND.value(), response["status"])
    }

    @Test
    fun findSpecificDataset() {
        val response = apiGet(port, "/concepts/$CONCEPT_ID_0", "application/rdf+json")
        assumeTrue(HttpStatus.OK.value() == response["status"])

        val expected = responseReader.parseTurtleFile("concept_0.ttl")
        val responseModel = responseReader.parseResponse(response["body"] as String, Lang.RDFJSON.name)

        assertTrue(expected.isIsomorphicWith(responseModel))
    }

    @Test
    fun findSpecificCatalog() {
        val response = apiGet(port, "/concepts/collections/$CONCEPT_COLLECTION_ID", "application/trig")
        assumeTrue(HttpStatus.OK.value() == response["status"])

        val expected = responseReader.parseTurtleFile("fdk_ready_concepts.ttl")
        val responseModel = responseReader.parseResponse(response["body"] as String, Lang.TRIG.name)

        assertTrue(expected.isIsomorphicWith(responseModel))
    }

    @Test
    fun findAll() {
        val response = apiGet(port, "/concepts", "text/n3")
        assumeTrue(HttpStatus.OK.value() == response["status"])

        val expected = responseReader.parseTurtleFile("fdk_ready_concepts.ttl")
        val responseModel = responseReader.parseResponse(response["body"] as String, Lang.N3.name)

        assertTrue(expected.isIsomorphicWith(responseModel))
    }

}
