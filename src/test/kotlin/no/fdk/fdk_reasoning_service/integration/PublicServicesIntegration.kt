package no.fdk.fdk_reasoning_service.integration

import no.fdk.fdk_reasoning_service.service.PublicServiceService
import no.fdk.fdk_reasoning_service.utils.ApiTestContext
import no.fdk.fdk_reasoning_service.utils.PUBLIC_SERVICE_ID_0
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
class PublicServicesIntegration : ApiTestContext() {
    private val responseReader = TestResponseReader()

    @Autowired
    private lateinit var publicServiceService: PublicServiceService

    @BeforeAll
    fun runReasoning() {
        publicServiceService.reasonHarvestedPublicServices()
    }

    @Test
    fun idDoesNotExist() {
        val response = apiGet(port, "/public-services/123", "text/turtle")
        assertEquals(HttpStatus.NOT_FOUND.value(), response["status"])
    }

    @Test
    fun findSpecific() {
        val response = apiGet(port, "/public-services/$PUBLIC_SERVICE_ID_0", "application/rdf+xml")
        assumeTrue(HttpStatus.OK.value() == response["status"])

        val expected = responseReader.parseTurtleFile("public_service_0.ttl")
        val responseModel = responseReader.parseResponse(response["body"] as String, Lang.RDFXML.name)

        assertTrue(expected.isIsomorphicWith(responseModel))
    }

    @Test
    fun findAll() {
        val response = apiGet(port, "/public-services", "text/n3")
        assumeTrue(HttpStatus.OK.value() == response["status"])

        val expected = responseReader.parseTurtleFile("fdk_ready_public_services.ttl")
        val responseModel = responseReader.parseResponse(response["body"] as String, Lang.N3.name)

        assertTrue(expected.isIsomorphicWith(responseModel))
    }

}
