package no.fdk.fdk_reasoning_service.unit

import com.nhaarman.mockitokotlin2.*
import no.fdk.fdk_reasoning_service.model.TurtleDBO
import no.fdk.fdk_reasoning_service.service.*
import no.fdk.fdk_reasoning_service.utils.TestResponseReader
import no.fdk.fdk_reasoning_service.utils.allEventIds
import org.apache.jena.riot.Lang
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.data.mongodb.core.MongoTemplate
import java.util.*
import kotlin.test.assertTrue

@Tag("unit")
class Events {
    private val eventMongoTemplate: MongoTemplate = mock()
    private val reasoningService: ReasoningService = mock()
    private val eventService = EventService(reasoningService, eventMongoTemplate)
    private val responseReader = TestResponseReader()

    @Test
    fun testEvents() {
        val eventsModel = responseReader.parseTurtleFile("fdk_ready_events.ttl")
        whenever(eventMongoTemplate.findById<TurtleDBO>(any(), any(), any()))
            .thenReturn(TurtleDBO("unionId", gzip("")))
        whenever(reasoningService.catalogReasoning(any(), any()))
            .thenReturn(eventsModel)

        eventService.reasonHarvestedEvents()

        argumentCaptor<TurtleDBO, String>().apply {
            verify(eventMongoTemplate, times(11)).save(first.capture(), second.capture())
            Assertions.assertEquals(allEventIds, first.allValues.map { it.id })
            Assertions.assertEquals(Collections.nCopies(11, "fdkEvent"), second.allValues)

            val savedUnion = parseRDFResponse(ungzip(first.firstValue.turtle), Lang.TURTLE, "")
            assertTrue(eventsModel.isIsomorphicWith(savedUnion))

            val expectedEvent = responseReader.parseTurtleFile("event_0.ttl")
            val savedEvent = parseRDFResponse(ungzip(first.allValues[8].turtle), Lang.TURTLE, "")
            assertTrue(expectedEvent.isIsomorphicWith(savedEvent))
        }
    }

}
