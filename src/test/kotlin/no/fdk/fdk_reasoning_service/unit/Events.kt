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
class Events {
    private val eventMongoTemplate: MongoTemplate = mock()
    private val reasoningService: ReasoningService = mock()
    private val eventService = EventService(reasoningService, eventMongoTemplate)
    private val responseReader = TestResponseReader()

    @Test
    fun testEvents() {
        val eventModel = responseReader.parseTurtleFile("fdk_ready_event_0.ttl")
        whenever(eventMongoTemplate.findById<TurtleDBO>(any(), any(), any()))
            .thenReturn(TurtleDBO(EVENT_ID_0, gzip("")))
        whenever(reasoningService.catalogReasoning(any(), any(), any()))
            .thenReturn(eventModel)

        val report = eventService.reasonReportedChanges(EVENT_REPORT, RDF_DATA, TEST_DATE)

        argumentCaptor<TurtleDBO, String>().apply {
            verify(eventMongoTemplate, times(2)).save(first.capture(), second.capture())
            Assertions.assertEquals(listOf(EVENT_ID_0, EVENT_ID_1), first.allValues.map { it.id })
            Assertions.assertEquals(Collections.nCopies(2, "fdkEvent"), second.allValues)

            val savedEvent = parseRDFResponse(ungzip(first.allValues[0].turtle), Lang.TURTLE, "")
            assertTrue(eventModel.isIsomorphicWith(savedEvent))
        }

        val expectedReport = ReasoningReport(
            id = "id", url = "https://events.com", dataType = "event",
            harvestError = false, startTime = "2022-05-05 07:39:41 +0200", endTime = report.endTime,
            changedCatalogs = emptyList(),
            changedResources = listOf(
                FdkIdAndUri(EVENT_ID_0, "https://events.com/$EVENT_ID_0"),
                FdkIdAndUri(EVENT_ID_1, "https://events.com/$EVENT_ID_1")))
        assertEquals(expectedReport, report)
    }

    @Test
    fun testEventsUnion() {
        whenever(eventMongoTemplate.findAll<TurtleDBO>("fdkEvent"))
            .thenReturn(listOf(
                TurtleDBO(EVENT_ID_0, gzip(responseReader.readFile("fdk_ready_event_0.ttl"))),
                TurtleDBO(EVENT_ID_1, gzip(responseReader.readFile("event_1.ttl")))))

        eventService.updateUnion()

        argumentCaptor<TurtleDBO, String>().apply {
            verify(eventMongoTemplate, times(1)).save(first.capture(), second.capture())
            Assertions.assertEquals(UNION_ID, first.firstValue.id)
            Assertions.assertEquals(Collections.nCopies(1, "fdkEvent"), second.allValues)

            val expectedUnion = responseReader.parseTurtleFile("fdk_ready_events.ttl")
            val savedUnion = parseRDFResponse(ungzip(first.firstValue.turtle), Lang.TURTLE, "")
            assertTrue(expectedUnion.isIsomorphicWith(savedUnion))
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
