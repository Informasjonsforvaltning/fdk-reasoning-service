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
import java.util.*
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
        val reasonedConcepts = responseReader.parseTurtleFile("reasoned_concepts.ttl")
        val expectedSaved = responseReader.parseTurtleFile("saved_concepts.ttl")
        whenever(conceptMongoTemplate.findById<TurtleDBO>(any(), any(), any()))
            .thenReturn(TurtleDBO("collectionId", gzip("")))
        whenever(reasoningService.catalogReasoning(any(), any(), any()))
            .thenReturn(reasonedConcepts)

        val report = conceptService.reasonReportedChanges(CONCEPT_REPORT, RDF_DATA, TEST_DATE)

        argumentCaptor<TurtleDBO, String>().apply {
            verify(conceptMongoTemplate, times(3)).save(first.capture(), second.capture())
            assertEquals(allConceptIds.sorted(), first.allValues.map { it.id }.sorted())
            assertEquals(listOf("fdkCollections", "fdkConcepts", "fdkConcepts"), second.allValues)

            val savedCollection = parseRDFResponse(ungzip(first.firstValue.turtle), Lang.TURTLE, "")
            assertTrue(expectedSaved.isIsomorphicWith(savedCollection))

            val expectedConcept = responseReader.parseTurtleFile("concept_0.ttl")
            val savedConcept = parseRDFResponse(ungzip(first.allValues.sortedBy { it.id }.last().turtle), Lang.TURTLE, "")
            assertTrue(expectedConcept.isIsomorphicWith(savedConcept))

            val expectedReport = ReasoningReport(
                id = "id", url = "https://concepts.com", dataType = "concept",
                harvestError = false, startTime = "2022-05-05 07:39:41 +0200", endTime = report.endTime,
                changedCatalogs = listOf(FdkIdAndUri(CONCEPT_COLLECTION_ID, "https://concepts.com/$CONCEPT_COLLECTION_ID")),
                changedResources = emptyList())
            assertEquals(expectedReport, report)
        }
    }

    @Test
    fun testConceptsUnion() {
        whenever(conceptMongoTemplate.findAll<TurtleDBO>("fdkCollections"))
            .thenReturn(listOf(TurtleDBO(CONCEPT_COLLECTION_ID, gzip(responseReader.readFile("reasoned_concepts.ttl")))))

        conceptService.updateUnion()

        argumentCaptor<TurtleDBO, String>().apply {
            verify(conceptMongoTemplate, times(1)).save(first.capture(), second.capture())
            Assertions.assertEquals(UNION_ID, first.firstValue.id)
            Assertions.assertEquals(Collections.nCopies(1, "fdkCollections"), second.allValues)

            val expectedUnion = responseReader.parseTurtleFile("reasoned_concepts.ttl")
            val savedUnion = parseRDFResponse(ungzip(first.firstValue.turtle), Lang.TURTLE, "")
            assertTrue(expectedUnion.isIsomorphicWith(savedUnion))
        }
    }

    @Test
    fun testConceptsError() {
        whenever(conceptMongoTemplate.findById<TurtleDBO>(any(), any(), any()))
            .thenReturn(TurtleDBO("unionId", gzip("")))
        whenever(reasoningService.catalogReasoning(any(), any(), any()))
            .thenThrow(RuntimeException("Error message"))

        val report = assertDoesNotThrow { conceptService.reasonReportedChanges(CONCEPT_REPORT, RDF_DATA, TEST_DATE) }

        argumentCaptor<TurtleDBO, String>().apply {
            verify(conceptMongoTemplate, times(0)).save(first.capture(), second.capture())
        }

        val expectedReport = ReasoningReport(
            id = "id", url = "https://concepts.com", dataType = "concept",
            harvestError = true, errorMessage = "Error message",
            startTime = "2022-05-05 07:39:41 +0200", endTime = report.endTime,
            changedCatalogs = emptyList(), changedResources = emptyList())
        assertEquals(expectedReport, report)
    }

}
