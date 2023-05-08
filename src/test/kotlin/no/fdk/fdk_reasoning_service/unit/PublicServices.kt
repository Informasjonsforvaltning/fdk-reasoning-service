package no.fdk.fdk_reasoning_service.unit

import no.fdk.fdk_reasoning_service.model.ReasoningReport
import no.fdk.fdk_reasoning_service.model.TurtleDBO
import no.fdk.fdk_reasoning_service.service.*
import no.fdk.fdk_reasoning_service.utils.*
import org.apache.jena.riot.Lang
import org.junit.jupiter.api.*
import org.mockito.kotlin.*
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
        val catalog0 = responseReader.parseTurtleFile("reasoned_public_service_catalog_0.ttl")
        whenever(publicServiceMongoTemplate.findById<TurtleDBO>(any(), any(), any()))
            .thenReturn(PUBLIC_SERVICE_CATALOG_0_DATA)
        whenever(reasoningService.catalogReasoning(any(), any(), any()))
            .thenReturn(catalog0)

        val report = publicServiceService.reasonReportedChanges(PUBLIC_SERVICE_REPORT_0, RDF_DATA, TEST_DATE)

        argumentCaptor<TurtleDBO, String>().apply {
            verify(publicServiceMongoTemplate, times(2)).save(first.capture(), second.capture())
            Assertions.assertEquals(listOf(PUBLIC_SERVICE_CATALOG_0_ID, PUBLIC_SERVICE_ID_0), first.allValues.map { it.id })
            Assertions.assertEquals(listOf("reasonedCatalog", "reasonedPublicService"), second.allValues)

            val expected = responseReader.parseTurtleFile("saved_public_service_catalog_0.ttl")
            val savedCatalog0 = parseRDFResponse(ungzip(first.allValues[0].turtle), Lang.TURTLE, "")
            assertTrue(expected.isIsomorphicWith(savedCatalog0))
        }

        val expectedReport = ReasoningReport(
            id = "id", url = "https://public-services.com", dataType = "publicService",
            harvestError = false, startTime = "2022-05-05 07:39:41 +0200", endTime = report.endTime,
            changedCatalogs = PUBLIC_SERVICE_REPORT_0.changedCatalogs,
            changedResources = PUBLIC_SERVICE_REPORT_0.changedResources)
        assertEquals(expectedReport, report)
    }

    @Test
    fun testPublicServicesUnion() {
        whenever(publicServiceMongoTemplate.findAll<TurtleDBO>("reasonedCatalog"))
            .thenReturn(listOf(
                TurtleDBO(PUBLIC_SERVICE_ID_0, gzip(responseReader.readFile("reasoned_public_service_catalog_0.ttl"))),
                TurtleDBO(PUBLIC_SERVICE_ID_1, gzip(responseReader.readFile("reasoned_public_service_catalog_1.ttl")))))
        whenever(publicServiceMongoTemplate.findAll<TurtleDBO>("reasonedPublicService"))
            .thenReturn(listOf(
                TurtleDBO(PUBLIC_SERVICE_ID_0, gzip(responseReader.readFile("reasoned_public_service_0.ttl"))),
                TurtleDBO(PUBLIC_SERVICE_ID_1, gzip(responseReader.readFile("reasoned_public_service_1.ttl")))))

        publicServiceService.updateUnion()

        argumentCaptor<TurtleDBO, String>().apply {
            verify(publicServiceMongoTemplate, times(2)).save(first.capture(), second.capture())
            Assertions.assertEquals(listOf(UNION_ID, UNION_ID), first.allValues.map { it.id })
            Assertions.assertEquals(listOf("reasonedCatalog", "reasonedPublicService"), second.allValues)

            val expectedCatalogUnion = responseReader.parseTurtleFile("reasoned_public_service_catalog_0.ttl")
                .union(responseReader.parseTurtleFile("reasoned_public_service_catalog_1.ttl"))
            val savedCatalogUnion = parseRDFResponse(ungzip(first.firstValue.turtle), Lang.TURTLE, "")
            assertTrue(expectedCatalogUnion.isIsomorphicWith(savedCatalogUnion))

            val expectedServiceUnion = responseReader.parseTurtleFile("reasoned_public_services.ttl")
            val savedServiceUnion = parseRDFResponse(ungzip(first.secondValue.turtle), Lang.TURTLE, "")
            assertTrue(expectedServiceUnion.isIsomorphicWith(savedServiceUnion))
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
