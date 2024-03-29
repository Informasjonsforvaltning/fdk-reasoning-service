package no.fdk.fdk_reasoning_service.integration

import no.fdk.fdk_reasoning_service.service.DataServiceService
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
class DataServicesIntegration : ApiTestContext() {
    private val responseReader = TestResponseReader()

    @Autowired
    private lateinit var dataServiceService: DataServiceService

    @BeforeAll
    fun runReasoning() {
        dataServiceService.reasonReportedChanges(DATA_SERVICE_REPORT, RDF_DATA, TEST_DATE)
        dataServiceService.updateUnion()
    }

    @Test
    fun idDoesNotExist() {
        val response = apiGet(port, "/data-services/123", "text/turtle")
        assertEquals(HttpStatus.NOT_FOUND.value(), response["status"])
    }

    @Test
    fun findSpecificDataservice() {
        val response = apiGet(port, "/data-services/$DATA_SERVICE_ID_0", "text/turtle")
        assumeTrue(HttpStatus.OK.value() == response["status"])

        val expected = responseReader.parseTurtleFile("saved_dataservice_0.ttl")
        val responseModel = responseReader.parseResponse(response["body"] as String, Lang.TURTLE.name)

        assertTrue(expected.isIsomorphicWith(responseModel))
    }

    @Test
    fun findSpecificCatalog() {
        val response = apiGet(port, "/data-services/catalogs/$DATA_SERVICE_CATALOG_ID", "application/n-quads")
        assumeTrue(HttpStatus.OK.value() == response["status"])

        val expected = responseReader.parseTurtleFile("saved_dataservices.ttl")
        val responseModel = responseReader.parseResponse(response["body"] as String, Lang.NQUADS.name)

        assertTrue(expected.isIsomorphicWith(responseModel))
    }

    @Test
    fun findAll() {
        val response = apiGet(port, "/data-services", "application/n-triples")
        assumeTrue(HttpStatus.OK.value() == response["status"])

        val expected = responseReader.parseTurtleFile("saved_dataservices.ttl")
        val responseModel = responseReader.parseResponse(response["body"] as String, Lang.NTRIPLES.name)

        assertTrue(expected.isIsomorphicWith(responseModel))
    }

}
