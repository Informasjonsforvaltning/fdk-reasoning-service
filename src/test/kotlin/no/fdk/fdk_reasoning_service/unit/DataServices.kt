package no.fdk.fdk_reasoning_service.unit

import com.nhaarman.mockitokotlin2.*
import no.fdk.fdk_reasoning_service.model.TurtleDBO
import no.fdk.fdk_reasoning_service.service.*
import no.fdk.fdk_reasoning_service.utils.TestResponseReader
import no.fdk.fdk_reasoning_service.utils.allDataServiceIds
import org.apache.jena.riot.Lang
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.data.mongodb.core.MongoTemplate
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
            .thenReturn(TurtleDBO("unionId", gzip("")))
        whenever(reasoningService.catalogReasoning(any(), any()))
            .thenReturn(dataServicesModel)

        dataServiceService.reasonHarvestedDataServices()

        argumentCaptor<TurtleDBO, String>().apply {
            verify(dataServiceMongoTemplate, times(4)).save(first.capture(), second.capture())
            assertEquals(allDataServiceIds, first.allValues.map { it.id })
            assertEquals(listOf("fdkCatalogs", "fdkCatalogs", "fdkServices", "fdkServices"), second.allValues)

            val savedUnion = parseRDFResponse(ungzip(first.firstValue.turtle), Lang.TURTLE, "")
            assertTrue(dataServicesModel.isIsomorphicWith(savedUnion))

            val savedCatalog = parseRDFResponse(ungzip(first.secondValue.turtle), Lang.TURTLE, "")
            assertTrue(dataServicesModel.isIsomorphicWith(savedCatalog))

            val expectedDataService = responseReader.parseTurtleFile("dataservice_0.ttl")
            val savedDataService = parseRDFResponse(ungzip(first.thirdValue.turtle), Lang.TURTLE, "")
            assertTrue(expectedDataService.isIsomorphicWith(savedDataService))
        }
    }

}
