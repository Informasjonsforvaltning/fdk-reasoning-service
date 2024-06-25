package no.fdk.fdk_reasoning_service.unit

import io.mockk.every
import io.mockk.mockk
import no.fdk.fdk_reasoning_service.cache.ReferenceDataCache
import no.fdk.fdk_reasoning_service.model.CatalogType
import no.fdk.fdk_reasoning_service.rdf.CV
import no.fdk.fdk_reasoning_service.service.ThemeService
import no.fdk.fdk_reasoning_service.utils.TestResponseReader
import org.apache.jena.vocabulary.DCAT
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

@Tag("unit")
class Theme {
    private val referenceDataCache: ReferenceDataCache = mockk()
    private val themeService = ThemeService(referenceDataCache)
    private val responseReader = TestResponseReader()

    init { setupReferenceDataCacheMocks() }

    private fun setupReferenceDataCacheMocks() {
        every { referenceDataCache.los() } returns responseReader
            .parseTurtleFile("rdf-data/reference-data/los.ttl")
        every { referenceDataCache.eurovocs() } returns responseReader
            .parseTurtleFile("rdf-data/reference-data/eurovocs.ttl")
        every { referenceDataCache.dataThemes() } returns responseReader
            .parseTurtleFile("rdf-data/reference-data/data_themes.ttl")
    }

    @Nested
    internal inner class DataService {

        @Test
        fun `test no extra theme triples are added when not applicable`() {
            val result = themeService.reason(
                responseReader.parseTurtleFile("rdf-data/input-graphs/data_service.ttl"),
                CatalogType.DATASERVICES,
            )
            val expected = responseReader.parseTurtleFile("rdf-data/expected/empty_graph.ttl")

            assertTrue(result.isIsomorphicWith(expected))
        }

        @Test
        fun `test theme triples are added from reference data`() {
            val dataServiceURI = "https://dataservice-catalog.staging.fellesdatakatalog.digdir.no/data-services/5f48b38626087749e9be175e"
            val input = responseReader.parseTurtleFile("rdf-data/input-graphs/data_service.ttl")
            input.add(input.getResource(dataServiceURI), DCAT.theme, input.createResource("https://psi.norge.no/los/tema/energi"))

            val result = themeService.reason(input, CatalogType.DATASERVICES)
            val expected = responseReader.parseTurtleFile("rdf-data/expected/theme-data/data_service.ttl")

            assertTrue(result.isIsomorphicWith(expected))
        }
    }


    @Nested
    internal inner class Dataset {
        val datasetURI = "https://dataservice-catalog.staging.fellesdatakatalog.digdir.no/data-services/5f48b38626087749e9be175e"

        @Test
        fun `test no extra theme triples are added when not applicable`() {
            val result = themeService.reason(
                responseReader.parseTurtleFile("rdf-data/input-graphs/dataset.ttl"),
                CatalogType.DATASETS,
            )
            val expected = responseReader.parseTurtleFile("rdf-data/expected/empty_graph.ttl")

            assertTrue(result.isIsomorphicWith(expected))
        }

        @Test
        fun `test data theme`() {
            val input = responseReader.parseTurtleFile("rdf-data/input-graphs/dataset.ttl")
            input.add(input.getResource(datasetURI), DCAT.theme, input.createResource("http://publications.europa.eu/resource/authority/data-theme/ENER"))

            val result = themeService.reason(input, CatalogType.DATASETS)
            val expected = responseReader.parseTurtleFile("rdf-data/expected/theme-data/dataset_data_theme.ttl")

            assertTrue(result.isIsomorphicWith(expected))
        }

        @Test
        fun `test los with exact match`() {
            val input = responseReader.parseTurtleFile("rdf-data/input-graphs/dataset.ttl")
            input.add(input.getResource(datasetURI), DCAT.theme, input.createResource("https://psi.norge.no/los/tema/energi"))

            val result = themeService.reason(input, CatalogType.DATASETS)
            val expected = responseReader.parseTurtleFile("rdf-data/expected/theme-data/dataset_los_exact_match.ttl")

            assertTrue(result.isIsomorphicWith(expected))
        }

        @Test
        fun `test los with close match`() {
            val input = responseReader.parseTurtleFile("rdf-data/input-graphs/dataset.ttl")
            input.add(input.getResource(datasetURI), DCAT.theme, input.createResource("https://psi.norge.no/los/tema/natur-klima-og-miljo"))

            val result = themeService.reason(input, CatalogType.DATASETS)
            val expected = responseReader.parseTurtleFile("rdf-data/expected/theme-data/dataset_los_close_match.ttl")

            assertTrue(result.isIsomorphicWith(expected))
        }

        @Test
        fun `test los with broad match`() {
            val input = responseReader.parseTurtleFile("rdf-data/input-graphs/dataset.ttl")
            input.add(input.getResource(datasetURI), DCAT.theme, input.createResource("https://psi.norge.no/los/tema/familie-og-barn"))

            val result = themeService.reason(input, CatalogType.DATASETS)
            val expected = responseReader.parseTurtleFile("rdf-data/expected/theme-data/dataset_los_broad_match.ttl")

            assertTrue(result.isIsomorphicWith(expected))
        }
    }

    @Nested
    internal inner class InformationModel {
        @Test
        fun `test no extra theme triples are added when not applicable`() {
            val result = themeService.reason(
                responseReader.parseTurtleFile("rdf-data/input-graphs/information_model.ttl"),
                CatalogType.INFORMATIONMODELS,
            )
            val expected = responseReader.parseTurtleFile("rdf-data/expected/empty_graph.ttl")

            assertTrue(result.isIsomorphicWith(expected))
        }

        @Test
        fun `test theme triples are added from reference data`() {
            val infoModelURI = "https://dataservice-catalog.staging.fellesdatakatalog.digdir.no/data-services/5f48b38626087749e9be175e"
            val input = responseReader.parseTurtleFile("rdf-data/input-graphs/information_model.ttl")
            input.add(input.getResource(infoModelURI), DCAT.theme, input.createResource("http://eurovoc.europa.eu/5073"))

            val result = themeService.reason(input, CatalogType.INFORMATIONMODELS)
            val expected = responseReader.parseTurtleFile("rdf-data/expected/theme-data/information_model.ttl")

            assertTrue(result.isIsomorphicWith(expected))
        }
    }

    @Nested
    internal inner class Service {
        @Test
        fun `test no extra theme triples are added when not applicable`() {
            val result = themeService.reason(
                responseReader.parseTurtleFile("rdf-data/input-graphs/service.ttl"),
                CatalogType.PUBLICSERVICES,
            )
            val expected = responseReader.parseTurtleFile("rdf-data/expected/empty_graph.ttl")

            assertTrue(result.isIsomorphicWith(expected))
        }

        @Test
        fun `test theme triples are added from reference data`() {
            val infoModelURI = "https://dataservice-catalog.staging.fellesdatakatalog.digdir.no/data-services/5f48b38626087749e9be175e"
            val input = responseReader.parseTurtleFile("rdf-data/input-graphs/service.ttl")
            input.add(input.getResource(infoModelURI), CV.thematicArea, input.createResource("http://eurovoc.europa.eu/272"))

            val result = themeService.reason(input, CatalogType.PUBLICSERVICES)
            val expected = responseReader.parseTurtleFile("rdf-data/expected/theme-data/service.ttl")

            assertTrue(result.isIsomorphicWith(expected))
        }
    }
}
