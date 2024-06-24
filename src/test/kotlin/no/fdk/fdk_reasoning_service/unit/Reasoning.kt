package no.fdk.fdk_reasoning_service.unit

import io.mockk.every
import io.mockk.mockk
import no.fdk.fdk_reasoning_service.model.CatalogType
import no.fdk.fdk_reasoning_service.service.DeductionService
import no.fdk.fdk_reasoning_service.service.OrganizationService
import no.fdk.fdk_reasoning_service.service.ReasoningService
import no.fdk.fdk_reasoning_service.service.ReferenceDataService
import no.fdk.fdk_reasoning_service.service.ThemeService
import no.fdk.fdk_reasoning_service.service.createRDFResponse
import no.fdk.fdk_reasoning_service.utils.*
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.riot.Lang
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue


@Tag("unit")
class Reasoning : ApiTestContext() {
    private val organizationService: OrganizationService = mockk()
    private val referenceDataService: ReferenceDataService = mockk()
    private val deductionService: DeductionService = mockk()
    private val themeService: ThemeService = mockk()
    private val reasoningService = ReasoningService(organizationService, referenceDataService, deductionService, themeService)
    private val responseReader = TestResponseReader()

    @Test
    fun testCombineReasonedGraphs() {
        val input = responseReader.parseTurtleFile("rdf-data/input-graphs/service.ttl")
            .union(responseReader.parseTurtleFile("rdf-data/input-graphs/service_extension.ttl"))
        val deductionsResult = responseReader.parseTurtleFile("rdf-data/expected/deduction-data/service.ttl")
        val orgResult = responseReader.parseTurtleFile("rdf-data/expected/org-data/service_org.ttl")
        val refDataResult = responseReader.parseTurtleFile("rdf-data/expected/reference-data/service.ttl")

        every { organizationService.reason(any(), any()) } returns orgResult
        every { deductionService.reason(any(), any()) } returns deductionsResult
        every { referenceDataService.reason(any(), any()) } returns refDataResult
        every { themeService.reason(any(), any()) } returns ModelFactory.createDefaultModel()
        val result = reasoningService.reasonGraph(input.createRDFResponse(Lang.TURTLE), CatalogType.PUBLICSERVICES)

        val expected = ModelFactory.createDefaultModel()
        expected.add(input)
        expected.add(deductionsResult)
        expected.add(orgResult)
        expected.add(refDataResult)

        assertTrue(responseReader.parseResponse(result, "TURTLE").isIsomorphicWith(expected))
    }
}
