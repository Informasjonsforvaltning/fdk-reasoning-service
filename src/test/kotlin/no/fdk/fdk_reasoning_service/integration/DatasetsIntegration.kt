package no.fdk.fdk_reasoning_service.integration

import no.fdk.fdk_reasoning_service.service.DatasetService
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
class DatasetsIntegration : ApiTestContext() {
    private val responseReader = TestResponseReader()

    @Autowired
    private lateinit var datasetService: DatasetService

    @BeforeAll
    fun runReasoning() {
        datasetService.reasonReportedChanges(DATASET_REPORT, RDF_DATA, TEST_DATE)
        datasetService.updateUnion()
    }

    @Test
    fun idDoesNotExist() {
        val response = apiGet(port, "/datasets/123", "text/turtle")
        assertEquals(HttpStatus.NOT_FOUND.value(), response["status"])
    }

    @Test
    fun findSpecificDataset() {
        val response = apiGet(port, "/datasets/$DATASET_ID_0", "application/ld+json")
        assumeTrue(HttpStatus.OK.value() == response["status"])

        val expected = responseReader.parseTurtleFile("dataset_0.ttl")
        val responseModel = responseReader.parseResponse(response["body"] as String, Lang.JSONLD.name)

        assertTrue(expected.isIsomorphicWith(responseModel))
    }

    @Test
    fun findSpecificCatalog() {
        val response = apiGet(port, "/datasets/catalogs/$DATASET_CATALOG_ID", "application/trix")
        assumeTrue(HttpStatus.OK.value() == response["status"])

        val expected = responseReader.parseTurtleFile("fdk_ready_datasets.ttl")
        val responseModel = responseReader.parseResponse(response["body"] as String, Lang.TRIX.name)

        assertTrue(expected.isIsomorphicWith(responseModel))
    }

    @Test
    fun findAll() {
        val response = apiGet(port, "/datasets", "text/turtle")
        assumeTrue(HttpStatus.OK.value() == response["status"])

        val expected = responseReader.parseTurtleFile("fdk_ready_datasets.ttl")
        val responseModel = responseReader.parseResponse(response["body"] as String, Lang.TURTLE.name)

        assertTrue(expected.isIsomorphicWith(responseModel))
    }

}
