package no.fdk.fdk_reasoning_service.unit

import no.fdk.fdk_reasoning_service.model.FdkIdAndUri
import no.fdk.fdk_reasoning_service.model.ReasoningReport
import no.fdk.fdk_reasoning_service.repository.InformationModelRepository
import no.fdk.fdk_reasoning_service.service.*
import no.fdk.fdk_reasoning_service.utils.*
import org.apache.jena.riot.Lang
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.mockito.kotlin.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Tag("unit")
class InformationModels {
    private val repository: InformationModelRepository = mock()
    private val reasoningService: ReasoningService = mock()
    private val infoModelService = InfoModelService(reasoningService, repository)
    private val responseReader = TestResponseReader()

    @Test
    fun testInformationModels() {
        val infoModelsUnion = responseReader.parseTurtleFile("reasoned_infomodels.ttl")
        whenever(repository.findHarvestedCatalog(any())).thenReturn("")
        whenever(reasoningService.catalogReasoning(any(), any(), any()))
            .thenReturn(infoModelsUnion)

        val report = infoModelService.reasonReportedChanges(INFOMODEL_REPORT, RDF_DATA, TEST_DATE)

        argumentCaptor<String, String>().apply {
            verify(repository, times(1)).saveCatalog(first.capture(), second.capture())
            val expectedCatalog = responseReader.parseTurtleFile("saved_infomodels.ttl")
            val savedCatalog = parseRDFResponse(first.firstValue, Lang.TURTLE, "")
            assertTrue(expectedCatalog.isIsomorphicWith(savedCatalog))
            assertEquals(INFOMODEL_CATALOG_ID, second.firstValue)
        }

        argumentCaptor<String, String>().apply {
            verify(repository, times(2)).saveInformationModel(first.capture(), second.capture())
            val expectedInfoModel = responseReader.parseTurtleFile("saved_infomodel_0.ttl")
            val savedInfoModel = parseRDFResponse(first.secondValue, Lang.TURTLE, "")
            assertTrue(expectedInfoModel.isIsomorphicWith(savedInfoModel))
            assertEquals(INFOMODEL_0_ID, second.secondValue)
        }

        val expectedReport = ReasoningReport(
            id = "id", url = "https://information-models.com", dataType = "informationmodel",
            harvestError = false, startTime = "2022-05-05 07:39:41 +0200", endTime = report.endTime,
            changedCatalogs = listOf(FdkIdAndUri(INFOMODEL_CATALOG_ID, "https://information-models.com/$INFOMODEL_CATALOG_ID")),
            changedResources = emptyList())
        assertEquals(expectedReport, report)
    }

    @Test
    fun testInformationModelsUnion() {
        whenever(repository.findCatalogs())
            .thenReturn(listOf(responseReader.readFile("reasoned_infomodels.ttl")))

        infoModelService.updateUnion()

        argumentCaptor<String>().apply {
            verify(repository, times(1)).saveReasonedUnion(capture())
            val expected = responseReader.parseTurtleFile("reasoned_infomodels.ttl")
            val savedUnion = parseRDFResponse(firstValue, Lang.TURTLE, "")
            assertTrue(expected.isIsomorphicWith(savedUnion))
        }
    }

    @Test
    fun testInformationModelsError() {
        whenever(repository.findHarvestedCatalog(any())).thenReturn("")
        whenever(reasoningService.catalogReasoning(any(), any(), any()))
            .thenThrow(RuntimeException("Error message"))

        val report = assertDoesNotThrow { infoModelService.reasonReportedChanges(INFOMODEL_REPORT, RDF_DATA, TEST_DATE) }

        argumentCaptor<String, String>().apply {
            verify(repository, times(0)).saveCatalog(first.capture(), second.capture())
        }

        argumentCaptor<String, String>().apply {
            verify(repository, times(0)).saveInformationModel(first.capture(), second.capture())
        }

        val expectedReport = ReasoningReport(
            id = "id", url = "https://information-models.com", dataType = "informationmodel",
            harvestError = true, errorMessage = "Error message",
            startTime = "2022-05-05 07:39:41 +0200", endTime = report.endTime,
            changedCatalogs = emptyList(), changedResources = emptyList())
        assertEquals(expectedReport, report)
    }

}
