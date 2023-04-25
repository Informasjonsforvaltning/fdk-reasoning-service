package no.fdk.fdk_reasoning_service.integration

import no.fdk.fdk_reasoning_service.repository.InformationModelRepository
import no.fdk.fdk_reasoning_service.service.InfoModelService
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
class InformationModelsIntegration : ApiTestContext() {
    private val responseReader = TestResponseReader()

    @Autowired
    private lateinit var repository: InformationModelRepository

    @Autowired
    private lateinit var infoModelService: InfoModelService

    @BeforeAll
    fun runReasoning() {
        repository.saveContent(responseReader.readFile("infomodels.ttl"), "catalog-$INFOMODEL_CATALOG_ID")
        infoModelService.reasonReportedChanges(INFOMODEL_REPORT, RDF_DATA, TEST_DATE)
        infoModelService.updateUnion()
    }

    @Test
    fun idDoesNotExist() {
        val response = apiGet(port, "/information-models/123", "text/turtle")
        assertEquals(HttpStatus.NOT_FOUND.value(), response["status"])
    }

    @Test
    fun findSpecificDataset() {
        val response = apiGet(port, "/information-models/$INFOMODEL_0_ID", "application/ld+json")
        assumeTrue(HttpStatus.OK.value() == response["status"])

        val expected = responseReader.parseTurtleFile("infomodel_0.ttl")
        val responseModel = responseReader.parseResponse(response["body"] as String, Lang.JSONLD.name)

        assertTrue(expected.isIsomorphicWith(responseModel))
    }

    @Test
    fun findSpecificCatalog() {
        val response = apiGet(port, "/information-models/catalogs/$INFOMODEL_CATALOG_ID", "application/trix")
        assumeTrue(HttpStatus.OK.value() == response["status"])

        val expected = responseReader.parseTurtleFile("reasoned_infomodels.ttl")
        val responseModel = responseReader.parseResponse(response["body"] as String, Lang.TRIX.name)

        assertTrue(expected.isIsomorphicWith(responseModel))
    }

    @Test
    fun findAll() {
        val response = apiGet(port, "/information-models", "text/turtle")
        assumeTrue(HttpStatus.OK.value() == response["status"])

        val expected = responseReader.parseTurtleFile("reasoned_infomodels.ttl")
        val responseModel = responseReader.parseResponse(response["body"] as String, Lang.TURTLE.name)

        assertTrue(expected.isIsomorphicWith(responseModel))
    }

}
