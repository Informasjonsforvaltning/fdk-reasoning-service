package no.fdk.fdk_reasoning_service.unit

import com.nhaarman.mockitokotlin2.*
import no.fdk.fdk_reasoning_service.model.FdkIdAndUri
import no.fdk.fdk_reasoning_service.model.ReasoningReport
import no.fdk.fdk_reasoning_service.model.TurtleDBO
import no.fdk.fdk_reasoning_service.service.*
import no.fdk.fdk_reasoning_service.utils.*
import org.apache.jena.riot.Lang
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.findAll
import java.util.Collections
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Tag("unit")
class DataServices {
    private val dataServiceMongoTemplate: MongoTemplate = mock()
    private val reasoningService: ReasoningService = mock()
    private val dataServiceService = DataServiceService(reasoningService, dataServiceMongoTemplate)
    private val responseReader = TestResponseReader()

    @Test
    fun testDataServices() {
        val dataServicesModel = responseReader.parseTurtleFile("fdk_ready_dataservices.ttl")
        whenever(dataServiceMongoTemplate.findById<TurtleDBO>(any(), any(), any()))
            .thenReturn(TurtleDBO("catalog-$DATA_SERVICE_CATALOG_ID", gzip("")))
        whenever(reasoningService.catalogReasoning(any(), any(), any()))
            .thenReturn(dataServicesModel)

        val report = dataServiceService.reasonReportedChanges(DATA_SERVICE_REPORT, RDF_DATA, TEST_DATE)

        argumentCaptor<TurtleDBO, String>().apply {
            verify(dataServiceMongoTemplate, times(3)).save(first.capture(), second.capture())
            assertEquals(allDataServiceIds, first.allValues.map { it.id })
            assertEquals(listOf("fdkCatalogs", "fdkServices", "fdkServices"), second.allValues)

            val savedCatalog = parseRDFResponse(ungzip(first.firstValue.turtle), Lang.TURTLE, "")
            assertTrue(dataServicesModel.isIsomorphicWith(savedCatalog))

            val expectedDataService = responseReader.parseTurtleFile("dataservice_0.ttl")
            val savedDataService = parseRDFResponse(ungzip(first.secondValue.turtle), Lang.TURTLE, "")
            assertTrue(expectedDataService.isIsomorphicWith(savedDataService))

            val expectedReport = ReasoningReport(
                id = "id", url = "https://data-services.com", dataType = "dataservice",
                harvestError = false, startTime = "2022-05-05 07:39:41 +0200", endTime = report.endTime,
                changedCatalogs = listOf(FdkIdAndUri(DATA_SERVICE_CATALOG_ID, "https://data-services.com/$DATA_SERVICE_CATALOG_ID")),
                changedResources = emptyList())
            assertEquals(expectedReport, report)
        }
    }

    @Test
    fun testDataServicesUnion() {
        whenever(dataServiceMongoTemplate.findAll<TurtleDBO>("fdkCatalogs"))
            .thenReturn(listOf(TurtleDBO(DATA_SERVICE_CATALOG_ID, gzip(responseReader.readFile("fdk_ready_dataservices.ttl")))))

        dataServiceService.updateUnion()

        argumentCaptor<TurtleDBO, String>().apply {
            verify(dataServiceMongoTemplate, times(1)).save(first.capture(), second.capture())
            Assertions.assertEquals(UNION_ID, first.firstValue.id)
            Assertions.assertEquals(Collections.nCopies(1, "fdkCatalogs"), second.allValues)

            val expectedUnion = responseReader.parseTurtleFile("fdk_ready_dataservices.ttl")
            val savedUnion = parseRDFResponse(ungzip(first.firstValue.turtle), Lang.TURTLE, "")
            assertTrue(expectedUnion.isIsomorphicWith(savedUnion))
        }
    }

    @Test
    fun testDataServicesError() {
        whenever(dataServiceMongoTemplate.findById<TurtleDBO>(any(), any(), any()))
            .thenReturn(TurtleDBO("catalog-$DATA_SERVICE_CATALOG_ID", gzip("")))
        whenever(reasoningService.catalogReasoning(any(), any(), any()))
            .thenThrow(RuntimeException("Error message"))

        val report = assertDoesNotThrow { dataServiceService.reasonReportedChanges(DATA_SERVICE_REPORT, RDF_DATA, TEST_DATE) }

        argumentCaptor<TurtleDBO, String>().apply {
            verify(dataServiceMongoTemplate, times(0)).save(first.capture(), second.capture())
        }

        val expectedReport = ReasoningReport(
            id = "id", url = "https://data-services.com", dataType = "dataservice",
            harvestError = true, errorMessage = "Error message",
            startTime = "2022-05-05 07:39:41 +0200", endTime = report.endTime,
            changedCatalogs = emptyList(), changedResources = emptyList())
        assertEquals(expectedReport, report)
    }

}
