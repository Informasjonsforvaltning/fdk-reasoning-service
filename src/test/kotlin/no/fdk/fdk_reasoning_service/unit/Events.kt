package no.fdk.fdk_reasoning_service.unit

import no.fdk.fdk_reasoning_service.model.FdkIdAndUri
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
class Events {
    private val eventMongoTemplate: MongoTemplate = mock()
    private val reasoningService: ReasoningService = mock()
    private val eventService = EventService(reasoningService, eventMongoTemplate)
    private val responseReader = TestResponseReader()

    @Test
    fun testEvents() {
        val catalogModel = responseReader.parseTurtleFile("reasoned_event_catalogs.ttl")
        whenever(eventMongoTemplate.findById<TurtleDBO>(any(), any(), any()))
            .thenReturn(EVENT_CATALOG_DATA)
        whenever(reasoningService.catalogReasoning(any(), any(), any()))
            .thenReturn(catalogModel)

        val report = eventService.reasonReportedChanges(EVENT_REPORT, RDF_DATA, TEST_DATE)

        argumentCaptor<TurtleDBO, String>().apply {
            verify(eventMongoTemplate, times(3)).save(first.capture(), second.capture())
            Assertions.assertEquals(listOf(EVENT_CATALOG_ID, EVENT_ID_0, EVENT_ID_1), first.allValues.map { it.id })
            Assertions.assertEquals(listOf("reasonedCatalog", "reasonedEvent", "reasonedEvent"), second.allValues)

            val savedCatalog = parseRDFResponse(ungzip(first.allValues[0].turtle), Lang.TURTLE, "")
            assertTrue(catalogModel.isIsomorphicWith(savedCatalog))
        }

        val expectedReport = ReasoningReport(
            id = "id", url = "https://events.com", dataType = "event",
            harvestError = false, startTime = "2022-05-05 07:39:41 +0200", endTime = report.endTime,
            changedCatalogs = listOf(FdkIdAndUri(EVENT_CATALOG_ID, "http://localhost:5000/events/catalogs/$EVENT_CATALOG_ID")),
            changedResources = listOf(
                FdkIdAndUri(EVENT_ID_0, "https://events.com/$EVENT_ID_0"),
                FdkIdAndUri(EVENT_ID_1, "https://events.com/$EVENT_ID_1")))
        assertEquals(expectedReport, report)
    }

    @Test
    fun testEventsUnion() {
        whenever(eventMongoTemplate.findAll<TurtleDBO>("reasonedCatalog"))
            .thenReturn(listOf(TurtleDBO(EVENT_CATALOG_ID, gzip(responseReader.readFile("reasoned_event_catalogs.ttl")))))
        whenever(eventMongoTemplate.findAll<TurtleDBO>("reasonedEvent"))
            .thenReturn(listOf(
                TurtleDBO(EVENT_ID_0, gzip(responseReader.readFile("reasoned_event_0.ttl"))),
                TurtleDBO(EVENT_ID_1, gzip(responseReader.readFile("reasoned_event_1.ttl")))))

        eventService.updateUnion()

        argumentCaptor<TurtleDBO, String>().apply {
            verify(eventMongoTemplate, times(2)).save(first.capture(), second.capture())
            Assertions.assertEquals(UNION_ID, first.firstValue.id)
            Assertions.assertEquals(listOf("reasonedCatalog", "reasonedEvent"), second.allValues)

            val expectedEventUnion = responseReader.parseTurtleFile("reasoned_events.ttl")
            val expectedCatalogUnion = responseReader.parseTurtleFile("reasoned_event_catalogs.ttl")
            val savedCatalogUnion = parseRDFResponse(ungzip(first.firstValue.turtle), Lang.TURTLE, "")
            val savedEventUnion = parseRDFResponse(ungzip(first.secondValue.turtle), Lang.TURTLE, "")
            assertTrue(expectedEventUnion.isIsomorphicWith(savedEventUnion))
            assertTrue(expectedCatalogUnion.isIsomorphicWith(savedCatalogUnion))
        }
    }

    @Test
    fun testEventsError() {
        whenever(eventMongoTemplate.findById<TurtleDBO>(any(), any(), any()))
            .thenReturn(TurtleDBO(EVENT_ID_0, gzip("")))
        whenever(reasoningService.catalogReasoning(any(), any(), any()))
            .thenThrow(RuntimeException("Error message"))

        val report = assertDoesNotThrow { eventService.reasonReportedChanges(EVENT_REPORT, RDF_DATA, TEST_DATE) }

        argumentCaptor<TurtleDBO, String>().apply {
            verify(eventMongoTemplate, times(0)).save(first.capture(), second.capture())
        }

        val expectedReport = ReasoningReport(
            id = "id", url = "https://events.com", dataType = "event",
            harvestError = true, errorMessage = "Error message",
            startTime = "2022-05-05 07:39:41 +0200", endTime = report.endTime,
            changedCatalogs = emptyList(), changedResources =  emptyList())
        assertEquals(expectedReport, report)
    }

}
