package no.fdk.fdk_reasoning_service.unit

import no.fdk.fdk_reasoning_service.model.CatalogType
import no.fdk.fdk_reasoning_service.service.DeductionService
import no.fdk.fdk_reasoning_service.service.OrganizationService
import no.fdk.fdk_reasoning_service.service.ReasoningService
import no.fdk.fdk_reasoning_service.service.ReferenceDataService
import no.fdk.fdk_reasoning_service.service.createRDFResponse
import no.fdk.fdk_reasoning_service.utils.*
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.riot.Lang
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertTrue


@Tag("unit")
class Reasoning : ApiTestContext() {
    private val organizationService: OrganizationService = mock()
    private val referenceDataService: ReferenceDataService = mock()
    private val deductionService: DeductionService = mock()
    private val reasoningService = ReasoningService(organizationService, referenceDataService, deductionService)
    private val responseReader = TestResponseReader()

    @Test
    fun testCombineReasonedGraphs() {
        val input = responseReader.parseTurtleFile("rdf-data/input-graphs/service_1.ttl")
        val deductionsResult = responseReader.parseTurtleFile("rdf-data/expected/service_1_deductions.ttl")
        val orgResult = responseReader.parseTurtleFile("rdf-data/expected/org-data/service_1_org.ttl")
        val refDataResult = responseReader.parseTurtleFile("rdf-data/expected/reference-data/service_1_reference_data.ttl")

        whenever(organizationService.extraOrganizationTriples(any(), any()))
            .thenReturn(orgResult)
        whenever(deductionService.deductionsModel(any(), any()))
            .thenReturn(deductionsResult)
        whenever(referenceDataService.referenceDataModel(any(), any()))
            .thenReturn(refDataResult)
        val result = reasoningService.reasonGraph(input.createRDFResponse(Lang.TURTLE), CatalogType.PUBLICSERVICES)

        val expected = ModelFactory.createDefaultModel()
        expected.add(input)
        expected.add(deductionsResult)
        expected.add(orgResult)
        expected.add(refDataResult)

        assertTrue(responseReader.parseResponse(result, "TURTLE").isIsomorphicWith(expected))
    }
}
