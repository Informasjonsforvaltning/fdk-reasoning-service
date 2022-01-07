package no.fdk.fdk_reasoning_service.unit

import com.nhaarman.mockitokotlin2.*
import no.fdk.fdk_reasoning_service.model.TurtleDBO
import no.fdk.fdk_reasoning_service.service.*
import no.fdk.fdk_reasoning_service.utils.TestResponseReader
import no.fdk.fdk_reasoning_service.utils.allDatasetIds
import no.fdk.fdk_reasoning_service.utils.savedDatasetCollections
import org.apache.jena.riot.Lang
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.data.mongodb.core.MongoTemplate
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
        val datasetsModel = responseReader.parseTurtleFile("fdk_ready_datasets.ttl")
        whenever(datasetMongoTemplate.findById<TurtleDBO>(any(), any(), any()))
            .thenReturn(TurtleDBO("unionId", gzip("")))
        whenever(reasoningService.catalogReasoning(any(), any()))
            .thenReturn(datasetsModel)

        datasetService.reasonHarvestedDatasets()

        argumentCaptor<TurtleDBO, String>().apply {
            verify(datasetMongoTemplate, times(6)).save(first.capture(), second.capture())
            assertEquals(allDatasetIds, first.allValues.map { it.id })
            assertEquals(savedDatasetCollections, second.allValues)

            val savedUnion = parseRDFResponse(ungzip(first.firstValue.turtle), Lang.TURTLE, "")
            assertTrue(datasetsModel.isIsomorphicWith(savedUnion))

            val savedCatalog = parseRDFResponse(ungzip(first.secondValue.turtle), Lang.TURTLE, "")
            assertTrue(datasetsModel.isIsomorphicWith(savedCatalog))

            val expectedDataset = responseReader.parseTurtleFile("dataset_0.ttl")
            val savedDataset = parseRDFResponse(ungzip(first.thirdValue.turtle), Lang.TURTLE, "")
            assertTrue(expectedDataset.isIsomorphicWith(savedDataset))
        }
    }

    @Test
    fun testDatasetsError() {
        whenever(datasetMongoTemplate.findById<TurtleDBO>(any(), any(), any()))
            .thenReturn(TurtleDBO("unionId", gzip("")))
        whenever(reasoningService.catalogReasoning(any(), any()))
            .thenThrow(RuntimeException())

        assertThrows<Exception> { datasetService.reasonHarvestedDatasets() }

        argumentCaptor<TurtleDBO, String>().apply {
            verify(datasetMongoTemplate, times(0)).save(first.capture(), second.capture())
        }
    }

}
