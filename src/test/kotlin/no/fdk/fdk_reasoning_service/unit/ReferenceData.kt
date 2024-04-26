package no.fdk.fdk_reasoning_service.unit

import no.fdk.fdk_reasoning_service.cache.ReferenceDataCache
import no.fdk.fdk_reasoning_service.model.CatalogType
import no.fdk.fdk_reasoning_service.service.ReferenceDataService
import no.fdk.fdk_reasoning_service.utils.TestResponseReader
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertTrue

@Tag("unit")
class ReferenceData {
    private val referenceDataCache: ReferenceDataCache = mock()
    private val referenceDataService = ReferenceDataService(referenceDataCache)
    private val responseReader = TestResponseReader()

    @Nested
    internal inner class Concept {

        @Test
        fun `test no extra triples are added from reference data when not present as object in input`() {
            whenever(referenceDataCache.conceptStatuses())
                .thenReturn(responseReader.parseTurtleFile("rdf-data/reference-data/concept_statuses.ttl"))
            whenever(referenceDataCache.conceptSubjects())
                .thenReturn(responseReader.parseTurtleFile("rdf-data/reference-data/concept_subjects.ttl"))

            val result = referenceDataService.referenceDataModel(
                responseReader.parseTurtleFile("rdf-data/input-graphs/concept_0.ttl"),
                CatalogType.CONCEPTS,
            )
            val expected = responseReader.parseTurtleFile("rdf-data/expected/concept_0_reference_data.ttl")

            assertTrue(result.isIsomorphicWith(expected))
        }

        @Test
        fun `test concept status and concept subject triples are added from reference data`() {
            whenever(referenceDataCache.conceptStatuses())
                .thenReturn(responseReader.parseTurtleFile("rdf-data/reference-data/concept_statuses.ttl"))
            whenever(referenceDataCache.conceptSubjects())
                .thenReturn(responseReader.parseTurtleFile("rdf-data/reference-data/concept_subjects.ttl"))

            val result = referenceDataService.referenceDataModel(
                responseReader.parseTurtleFile("rdf-data/input-graphs/concept_1.ttl"),
                CatalogType.CONCEPTS,
            )
            val expected = responseReader.parseTurtleFile("rdf-data/expected/concept_1_reference_data.ttl")

            assertTrue(result.isIsomorphicWith(expected))
        }

    }

}
