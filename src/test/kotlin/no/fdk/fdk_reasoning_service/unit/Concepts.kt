package no.fdk.fdk_reasoning_service.unit

import com.nhaarman.mockitokotlin2.*
import no.fdk.fdk_reasoning_service.model.TurtleDBO
import no.fdk.fdk_reasoning_service.service.*
import no.fdk.fdk_reasoning_service.utils.TestResponseReader
import no.fdk.fdk_reasoning_service.utils.allConceptIds
import org.apache.jena.riot.Lang
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.data.mongodb.core.MongoTemplate
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Tag("unit")
class Concepts {
    private val conceptMongoTemplate: MongoTemplate = mock()
    private val reasoningService: ReasoningService = mock()
    private val conceptService = ConceptService(reasoningService, conceptMongoTemplate)
    private val responseReader = TestResponseReader()

    @Test
    fun testConcepts() {
        val conceptsModel = responseReader.parseTurtleFile("fdk_ready_concepts.ttl")
        whenever(conceptMongoTemplate.findById<TurtleDBO>(any(), any(), any()))
            .thenReturn(TurtleDBO("unionId", gzip("")))
        whenever(reasoningService.catalogReasoning(any(), any()))
            .thenReturn(conceptsModel)

        conceptService.reasonHarvestedConcepts()

        argumentCaptor<TurtleDBO, String>().apply {
            verify(conceptMongoTemplate, times(4)).save(first.capture(), second.capture())
            assertEquals(allConceptIds, first.allValues.map { it.id })
            assertEquals(listOf("fdkCollections", "fdkCollections", "fdkConcepts", "fdkConcepts"), second.allValues)

            val savedUnion = parseRDFResponse(ungzip(first.firstValue.turtle), Lang.TURTLE, "")
            assertTrue(conceptsModel.isIsomorphicWith(savedUnion))

            val savedCollection = parseRDFResponse(ungzip(first.secondValue.turtle), Lang.TURTLE, "")
            assertTrue(conceptsModel.isIsomorphicWith(savedCollection))

            val expectedConcept = responseReader.parseTurtleFile("concept_0.ttl")
            val savedConcept = parseRDFResponse(ungzip(first.allValues[3].turtle), Lang.TURTLE, "")
            assertTrue(expectedConcept.isIsomorphicWith(savedConcept))
        }
    }

}
