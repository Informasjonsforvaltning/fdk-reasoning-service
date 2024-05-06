package no.fdk.fdk_reasoning_service.unit

import no.fdk.fdk_reasoning_service.cache.ReferenceDataCache
import no.fdk.fdk_reasoning_service.model.CatalogType
import no.fdk.fdk_reasoning_service.service.DeductionService
import no.fdk.fdk_reasoning_service.utils.TestResponseReader
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

    init {
        setupReferenceDataCacheMocks()
    }

    private fun setupReferenceDataCacheMocks() {
        whenever(referenceDataCache.los())
            .thenReturn(responseReader.parseTurtleFile("rdf-data/reference-data/los.ttl"))
        whenever(referenceDataCache.eurovocs())
            .thenReturn(responseReader.parseTurtleFile("rdf-data/reference-data/eurovocs.ttl"))
        whenever(referenceDataCache.dataThemes())
            .thenReturn(responseReader.parseTurtleFile("rdf-data/reference-data/data_themes.ttl"))
    }

    @Nested
    internal inner class Concept {
        @Test
        fun `test add publisher rule`() {
            val result = deductionService.reason(
                responseReader.parseTurtleFile("rdf-data/input-graphs/concept_1.ttl"),
                CatalogType.CONCEPTS,
            )
            val expected = responseReader.parseTurtleFile("rdf-data/expected/concept_1_deductions.ttl")

            assertTrue(result.isIsomorphicWith(expected))
        }

        @Test
        fun `test no added triples when no rules are applicable`() {
            val result = deductionService.reason(
                responseReader.parseTurtleFile("rdf-data/input-graphs/concept_0.ttl"),
                CatalogType.CONCEPTS,
            )
            val expected = responseReader.parseTurtleFile("rdf-data/expected/empty_graph.ttl")

            assertTrue(result.isIsomorphicWith(expected))
        }
    }

    @Nested
    internal inner class DataService {
        @Test
        fun `test add publisher rule`() {
            val result = deductionService.reason(
                responseReader.parseTurtleFile("rdf-data/input-graphs/data_service_0.ttl"),
                CatalogType.DATASERVICES,
            )
            val expected = responseReader.parseTurtleFile("rdf-data/expected/data_service_0_deductions.ttl")

            assertTrue(result.isIsomorphicWith(expected))
        }

        @Test
        fun `test no added triples when no rules are applicable`() {
            val result = deductionService.reason(
                responseReader.parseTurtleFile("rdf-data/input-graphs/data_service_1.ttl"),
                CatalogType.DATASERVICES,
            )
            val expected = responseReader.parseTurtleFile("rdf-data/expected/empty_graph.ttl")

            assertTrue(result.isIsomorphicWith(expected))
        }
    }

    @Nested
    internal inner class Dataset {
        @Test
        fun `test add publisher rule`() {
            val result = deductionService.reason(
                responseReader.parseTurtleFile("rdf-data/input-graphs/dataset_0.ttl"),
                CatalogType.DATASETS,
            )
            val expected = responseReader.parseTurtleFile("rdf-data/expected/dataset_0_deductions.ttl")

            assertTrue(result.isIsomorphicWith(expected))
        }

        @Test
        fun `test add theme triples, associated data themes added from los & labels for all themes`() {
            val result = deductionService.reason(
                responseReader.parseTurtleFile("rdf-data/input-graphs/dataset_1.ttl"),
                CatalogType.DATASETS,
            )
            val expected = responseReader.parseTurtleFile("rdf-data/expected/dataset_1_deductions.ttl")

            assertTrue(result.isIsomorphicWith(expected))
        }

        @Test
        fun `test no added triples when no rules are applicable`() {
            val result = deductionService.reason(
                responseReader.parseTurtleFile("rdf-data/input-graphs/dataset_2.ttl"),
                CatalogType.DATASETS,
            )
            val expected = responseReader.parseTurtleFile("rdf-data/expected/empty_graph.ttl")

            assertTrue(result.isIsomorphicWith(expected))
        }

        @Test
        fun `test add isAuth = true, isOpen = true & transportPortal = true when applicable`() {
            val result = deductionService.reason(
                responseReader.parseTurtleFile("rdf-data/input-graphs/dataset_3.ttl"),
                CatalogType.DATASETS,
            )
            val expected = responseReader.parseTurtleFile("rdf-data/expected/dataset_3_deductions.ttl")

            assertTrue(result.isIsomorphicWith(expected))
        }
    }

    @Nested
    internal inner class InformationModel {
        @Test
        fun `test no added triples when no rules are applicable`() {
            val result = deductionService.reason(
                responseReader.parseTurtleFile("rdf-data/input-graphs/information_model_0.ttl"),
                CatalogType.INFORMATIONMODELS,
            )
            val expected = responseReader.parseTurtleFile("rdf-data/expected/empty_graph.ttl")

            assertTrue(result.isIsomorphicWith(expected))
        }

        @Test
        fun `test theme labels added`() {
            val result = deductionService.reason(
                responseReader.parseTurtleFile("rdf-data/input-graphs/information_model_1.ttl"),
                CatalogType.INFORMATIONMODELS,
            )
            val expected = responseReader.parseTurtleFile("rdf-data/expected/information_model_1_deductions.ttl")

            assertTrue(result.isIsomorphicWith(expected))
        }

        @Test
        fun `test add publisher rule`() {
            val result = deductionService.reason(
                responseReader.parseTurtleFile("rdf-data/input-graphs/information_model_2.ttl"),
                CatalogType.INFORMATIONMODELS,
            )
            val expected = responseReader.parseTurtleFile("rdf-data/expected/information_model_2_deductions.ttl")

            assertTrue(result.isIsomorphicWith(expected))
        }
    }

    @Nested
    internal inner class Service {
        @Test
        fun `test no added triples when no rules are applicable`() {
            val result = deductionService.reason(
                responseReader.parseTurtleFile("rdf-data/input-graphs/service_0.ttl"),
                CatalogType.PUBLICSERVICES,
            )
            val expected = responseReader.parseTurtleFile("rdf-data/expected/empty_graph.ttl")

            assertTrue(result.isIsomorphicWith(expected))
        }

        @Test
        fun `test theme labels added`() {
            val result = deductionService.reason(
                responseReader.parseTurtleFile("rdf-data/input-graphs/service_1.ttl"),
                CatalogType.PUBLICSERVICES,
            )
            val expected = responseReader.parseTurtleFile("rdf-data/expected/service_1_deductions.ttl")

            assertTrue(result.isIsomorphicWith(expected))
        }

        @Test
        fun `test add hasParticipate for participating agent`() {
            val result = deductionService.reason(
                responseReader.parseTurtleFile("rdf-data/input-graphs/service_2.ttl"),
                CatalogType.PUBLICSERVICES,
            )
            val expected = responseReader.parseTurtleFile("rdf-data/expected/service_2_deductions.ttl")

            assertTrue(result.isIsomorphicWith(expected))
        }
    }
}