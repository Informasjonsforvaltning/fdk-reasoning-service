package no.fdk.fdk_reasoning_service.unit
import no.fdk.fdk_reasoning_service.cache.ReferenceDataCache
import no.fdk.fdk_reasoning_service.model.CatalogType
import no.fdk.fdk_reasoning_service.service.DeductionService
import no.fdk.fdk_reasoning_service.utils.RDF_DATA
import no.fdk.fdk_reasoning_service.utils.TestResponseReader
import org.apache.jena.rdf.model.ModelFactory
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertTrue

@Tag("unit")
class Deduction {
    private val referenceDataCache: ReferenceDataCache = mock()
    private val deductionService = DeductionService(referenceDataCache)
    private val responseReader = TestResponseReader()
    @Nested
    internal inner class Dataset {
        @Test
        fun `test deduction`() {
            whenever(referenceDataCache.los()).thenReturn(
                ModelFactory.createDefaultModel()
            )
        }
    }

    @Nested
    internal inner class Concept {
        @Test
        fun `test add publisher rule`() {
            val result = deductionService.deductionsModel(
                responseReader.parseTurtleFile("rdf-data/input-graphs/concept_1.ttl"),
                CatalogType.CONCEPTS,
            )
            val expected = responseReader.parseTurtleFile("rdf-data/expected/concept_1_deductions.ttl")

            assertTrue(result.isIsomorphicWith(expected))
        }

        @Test
        fun `test add publisher rule do not add when already present`() {
            val result = deductionService.deductionsModel(
                responseReader.parseTurtleFile("rdf-data/input-graphs/concept_0.ttl"),
                CatalogType.CONCEPTS,
            )
            val expected = responseReader.parseTurtleFile("rdf-data/expected/empty_graph.ttl")

            assertTrue(result.isIsomorphicWith(expected))
        }
    }

    @Nested
    internal inner class Concept2 {
        @Test
        fun `test add publisher rule`() {
            val result = deductionService.deductionsModel(
                responseReader.parseTurtleFile("rdf-data/input-graphs/concept_1.ttl"),
                CatalogType.CONCEPTS,
            )
            val expected = responseReader.parseTurtleFile("rdf-data/expected/concept_1_deductions.ttl")

            assertTrue(result.isIsomorphicWith(expected))
        }

        @Test
        fun `test add publisher rule do not add when already present`() {
            val result = deductionService.deductionsModel(
                responseReader.parseTurtleFile("rdf-data/input-graphs/concept_0.ttl"),
                CatalogType.CONCEPTS,
            )
            val expected = responseReader.parseTurtleFile("rdf-data/expected/empty_graph.ttl")

            assertTrue(result.isIsomorphicWith(expected))
        }
    }
}