package no.fdk.fdk_reasoning_service.unit

import com.nhaarman.mockitokotlin2.*
import no.fdk.fdk_reasoning_service.model.FdkIdAndUri
import no.fdk.fdk_reasoning_service.model.ReasoningReport
import no.fdk.fdk_reasoning_service.model.TurtleDBO
import no.fdk.fdk_reasoning_service.service.*
import no.fdk.fdk_reasoning_service.utils.*
import org.apache.jena.riot.Lang
import org.junit.jupiter.api.*
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.findAll
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Tag("unit")
class PublicServices {
    private val publicServiceMongoTemplate: MongoTemplate = mock()
    private val reasoningService: ReasoningService = mock()
    private val publicServiceService = PublicServiceService(reasoningService, publicServiceMongoTemplate)
    private val responseReader = TestResponseReader()

    @Test
    fun testPublicServices() {
        val service0 = responseReader.parseTurtleFile("fdk_ready_public_service_0.ttl")
        whenever(publicServiceMongoTemplate.findById<TurtleDBO>(any(), any(), any()))
            .thenReturn(PUBLIC_SERVICE_0_DATA)
        whenever(reasoningService.catalogReasoning(any(), any(), any()))
            .thenReturn(service0)

        val report = publicServiceService.reasonReportedChanges(PUBLIC_SERVICE_REPORT, RDF_DATA, TEST_DATE)

        argumentCaptor<TurtleDBO, String>().apply {
            verify(publicServiceMongoTemplate, times(2)).save(first.capture(), second.capture())
            Assertions.assertEquals(listOf(PUBLIC_SERVICE_ID_0, PUBLIC_SERVICE_ID_1), first.allValues.map { it.id })
            Assertions.assertEquals(Collections.nCopies(2, "fdkPublicService"), second.allValues)

            val savedService = parseRDFResponse(ungzip(first.allValues[0].turtle), Lang.TURTLE, "")
            assertTrue(service0.isIsomorphicWith(savedService))
        }

        val expectedReport = ReasoningReport(
            id = "id", url = "https://public-services.com", dataType = "publicService",
            harvestError = false, startTime = "2022-05-05 07:39:41 +0200", endTime = report.endTime,
            changedCatalogs = emptyList(),
            changedResources = listOf(
                FdkIdAndUri(PUBLIC_SERVICE_ID_0, "https://public-services.com/$PUBLIC_SERVICE_ID_0"),
                FdkIdAndUri(PUBLIC_SERVICE_ID_1, "https://public-services.com/$PUBLIC_SERVICE_ID_1")))
        assertEquals(expectedReport, report)
    }

    @Test
    fun testPublicServicesUnion() {
        whenever(publicServiceMongoTemplate.findAll<TurtleDBO>("fdkPublicService"))
            .thenReturn(listOf(
                TurtleDBO(PUBLIC_SERVICE_ID_0, gzip(responseReader.readFile("fdk_ready_public_service_0.ttl"))),
                TurtleDBO(PUBLIC_SERVICE_ID_1, gzip(responseReader.readFile("fdk_ready_public_service_1.ttl")))))

        publicServiceService.updateUnion()

        argumentCaptor<TurtleDBO, String>().apply {
            verify(publicServiceMongoTemplate, times(1)).save(first.capture(), second.capture())
            Assertions.assertEquals(UNION_ID, first.firstValue.id)
            Assertions.assertEquals(Collections.nCopies(1, "fdkPublicService"), second.allValues)

            val expectedUnion = responseReader.parseTurtleFile("fdk_ready_public_services.ttl")
            val savedUnion = parseRDFResponse(ungzip(first.firstValue.turtle), Lang.TURTLE, "")
            assertTrue(expectedUnion.isIsomorphicWith(savedUnion))
        }
    }

    @Test
    fun testPublicServicesError() {
        whenever(publicServiceMongoTemplate.findById<TurtleDBO>(any(), any(), any()))
            .thenReturn(TurtleDBO("unionId", gzip("")))
        whenever(reasoningService.catalogReasoning(any(), any(), any()))
            .thenThrow(RuntimeException("Error message"))

        val report = assertDoesNotThrow { publicServiceService.reasonReportedChanges(PUBLIC_SERVICE_REPORT, RDF_DATA, TEST_DATE) }

        argumentCaptor<TurtleDBO, String>().apply {
            verify(publicServiceMongoTemplate, times(0)).save(first.capture(), second.capture())
        }

        val expectedReport = ReasoningReport(
            id = "id", url = "https://public-services.com", dataType = "publicService",
            harvestError = true, errorMessage = "Error message",
            startTime = "2022-05-05 07:39:41 +0200", endTime = report.endTime,
            changedCatalogs = emptyList(), changedResources =  emptyList())
        assertEquals(expectedReport, report)
    }

}
