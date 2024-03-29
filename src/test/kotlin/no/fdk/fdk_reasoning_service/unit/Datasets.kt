package no.fdk.fdk_reasoning_service.unit

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
import org.mockito.kotlin.*
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.findAll
import java.util.Collections
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Tag("unit")
class Datasets {
    private val datasetMongoTemplate: MongoTemplate = mock()
    private val reasoningService: ReasoningService = mock()
    private val datasetService = DatasetService(reasoningService, datasetMongoTemplate)
    private val responseReader = TestResponseReader()

    @Test
    fun testDatasets() {
        val datasetsModel = responseReader.parseTurtleFile("reasoned_datasets.ttl")
        whenever(datasetMongoTemplate.findById<TurtleDBO>(any(), any(), any()))
            .thenReturn(TurtleDBO("catalogId", gzip("")))
        whenever(reasoningService.catalogReasoning(any(), any(), any()))
            .thenReturn(datasetsModel)

        val report = datasetService.reasonReportedChanges(DATASET_REPORT, RDF_DATA, TEST_DATE)

        argumentCaptor<TurtleDBO, String>().apply {
            verify(datasetMongoTemplate, times(5)).save(first.capture(), second.capture())
            assertEquals(allDatasetIds.sorted(), first.allValues.map { it.id }.sorted())
            assertEquals(savedDatasetCollections, second.allValues)

            val expectedCatalog = responseReader.parseTurtleFile("saved_datasets.ttl")
            val savedCatalog = parseRDFResponse(ungzip(first.firstValue.turtle), Lang.TURTLE, "")
            assertTrue(expectedCatalog.isIsomorphicWith(savedCatalog))

            val expectedDataset = responseReader.parseTurtleFile("saved_dataset_0.ttl")
            val savedDataset = parseRDFResponse(ungzip(first.allValues.first { it.id == "4667277a-9d27-32c1-aed5-612fa601f393" }.turtle), Lang.TURTLE, "")
            assertTrue(expectedDataset.isIsomorphicWith(savedDataset))
        }

        val expectedReport = ReasoningReport(
            id = "id", url = "https://datasets.com", dataType = "dataset",
            harvestError = false, startTime = "2022-05-05 07:39:41 +0200", endTime = report.endTime,
            changedCatalogs = listOf(FdkIdAndUri(DATASET_CATALOG_ID, "https://datasets.com/$DATASET_CATALOG_ID")),
            changedResources = emptyList())
        assertEquals(expectedReport, report)
    }

    @Test
    fun testDatasetsUnion() {
        whenever(datasetMongoTemplate.findAll<TurtleDBO>("fdkCatalogs"))
            .thenReturn(listOf(TurtleDBO(DATA_SERVICE_CATALOG_ID, gzip(responseReader.readFile("reasoned_datasets.ttl")))))

        datasetService.updateUnion()

        argumentCaptor<TurtleDBO, String>().apply {
            verify(datasetMongoTemplate, times(1)).save(first.capture(), second.capture())
            Assertions.assertEquals(UNION_ID, first.firstValue.id)
            Assertions.assertEquals(Collections.nCopies(1, "fdkCatalogs"), second.allValues)

            val expectedUnion = responseReader.parseTurtleFile("reasoned_datasets.ttl")
            val savedUnion = parseRDFResponse(ungzip(first.firstValue.turtle), Lang.TURTLE, "")
            assertTrue(expectedUnion.isIsomorphicWith(savedUnion))
        }
    }

    @Test
    fun testDatasetsError() {
        whenever(datasetMongoTemplate.findById<TurtleDBO>(any(), any(), any()))
            .thenReturn(TurtleDBO("catalogId", gzip("")))
        whenever(reasoningService.catalogReasoning(any(), any(), any()))
            .thenThrow(RuntimeException("Error message"))

        val report = assertDoesNotThrow { datasetService.reasonReportedChanges(DATASET_REPORT, RDF_DATA, TEST_DATE) }

        argumentCaptor<TurtleDBO, String>().apply {
            verify(datasetMongoTemplate, times(0)).save(first.capture(), second.capture())
        }

        val expectedReport = ReasoningReport(
            id = "id", url = "https://datasets.com", dataType = "dataset",
            harvestError = true, errorMessage = "Error message",
            startTime = "2022-05-05 07:39:41 +0200", endTime = report.endTime,
            changedCatalogs = emptyList(), changedResources = emptyList())
        assertEquals(expectedReport, report)
    }

    @Test
    fun testDatasetSeries() {
        val datasetsModel = responseReader.parseTurtleFile("reasoned_dataset_series.ttl")
        whenever(datasetMongoTemplate.findById<TurtleDBO>(any(), any(), any()))
            .thenReturn(TurtleDBO("catalogId", gzip("")))
        whenever(reasoningService.catalogReasoning(any(), any(), any()))
            .thenReturn(datasetsModel)

        val report = datasetService.reasonReportedChanges(DATASET_REPORT, RDF_DATA, TEST_DATE)

        argumentCaptor<TurtleDBO, String>().apply {
            verify(datasetMongoTemplate, times(5)).save(first.capture(), second.capture())

            val expected = responseReader.parseTurtleFile("saved_dataset_series.ttl")
            val savedCatalog = parseRDFResponse(ungzip(first.firstValue.turtle), Lang.TURTLE, "")
            assertTrue(expected.isIsomorphicWith(savedCatalog))
        }

        val expectedReport = ReasoningReport(
            id = "id", url = "https://datasets.com", dataType = "dataset",
            harvestError = false, startTime = "2022-05-05 07:39:41 +0200", endTime = report.endTime,
            changedCatalogs = listOf(FdkIdAndUri(DATASET_CATALOG_ID, "https://datasets.com/$DATASET_CATALOG_ID")),
            changedResources = emptyList())
        assertEquals(expectedReport, report)
    }

}
