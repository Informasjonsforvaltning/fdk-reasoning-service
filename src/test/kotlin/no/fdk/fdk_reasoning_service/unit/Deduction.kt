package no.fdk.fdk_reasoning_service.unit

import no.fdk.fdk_reasoning_service.model.CatalogType
import no.fdk.fdk_reasoning_service.service.DeductionService
import no.fdk.fdk_reasoning_service.utils.TestResponseReader
import org.apache.jena.vocabulary.DCTerms
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

@Tag("unit")
class Deduction {
    private val deductionService = DeductionService()
    private val responseReader = TestResponseReader()

    @Nested
    internal inner class Concept {
        @Test
        fun `test add publisher rule`() {
            val input = responseReader.parseTurtleFile("rdf-data/input-graphs/concept.ttl")
            input.add(
                input.getResource("https://www.example.com/begrepskatalog/0"),
                DCTerms.publisher,
                input.createResource("https://data.brreg.no/enhetsregisteret/api/enheter/910244132")
            )

            val result = deductionService.reason(input, CatalogType.CONCEPTS)
            val expected = responseReader.parseTurtleFile("rdf-data/expected/deduction-data/concept.ttl")

            assertTrue(result.isIsomorphicWith(expected))
        }

        @Test
        fun `test no added triples when no rules are applicable`() {
            val input = responseReader.parseTurtleFile("rdf-data/input-graphs/concept.ttl")
            val result = deductionService.reason(input, CatalogType.CONCEPTS)
            val expected = responseReader.parseTurtleFile("rdf-data/expected/empty_graph.ttl")

            assertTrue(result.isIsomorphicWith(expected))
        }
    }

    @Nested
    internal inner class DataService {
        @Test
        fun `test add publisher rule`() {
            val input = responseReader.parseTurtleFile("rdf-data/input-graphs/data_service.ttl")
            input.add(
                input.getResource("https://dataservice-catalog.staging.fellesdatakatalog.digdir.no/catalogs/910298062"),
                DCTerms.publisher,
                input.createResource("https://organization-catalog.staging.fellesdatakatalog.digdir.no/organizations/910298062")
            )

            val result = deductionService.reason(input, CatalogType.DATASERVICES)
            val expected = responseReader.parseTurtleFile("rdf-data/expected/deduction-data/data_service.ttl")

            assertTrue(result.isIsomorphicWith(expected))
        }

        @Test
        fun `test no added triples when no rules are applicable`() {
            val input = responseReader.parseTurtleFile("rdf-data/input-graphs/data_service.ttl")
            val result = deductionService.reason(input, CatalogType.DATASERVICES)
            val expected = responseReader.parseTurtleFile("rdf-data/expected/empty_graph.ttl")

            assertTrue(result.isIsomorphicWith(expected))
        }
    }

    @Nested
    internal inner class Dataset {
        @Test
        fun `test add publisher rule`() {
            val input = responseReader.parseTurtleFile("rdf-data/input-graphs/dataset.ttl")
            input.add(
                input.getResource("http://registration-api:8080/catalogs/910298062"),
                DCTerms.publisher,
                input.createResource("https://organization-catalog.staging.fellesdatakatalog.digdir.no/organizations/910298062")
            )

            val result = deductionService.reason(input, CatalogType.DATASETS)
            val expected = responseReader.parseTurtleFile("rdf-data/expected/deduction-data/dataset_catalog_publisher.ttl")

            assertTrue(result.isIsomorphicWith(expected))
        }

        @Test
        fun `test no added triples when no rules are applicable`() {
            val input = responseReader.parseTurtleFile("rdf-data/input-graphs/dataset.ttl")
            val result = deductionService.reason(input, CatalogType.DATASETS)
            val expected = responseReader.parseTurtleFile("rdf-data/expected/empty_graph.ttl")

            assertTrue(result.isIsomorphicWith(expected))
        }

        @Test
        fun `test add isAuth = true, isOpen = true & transportPortal = true when applicable`() {
            val input = responseReader.parseTurtleFile("rdf-data/input-graphs/dataset.ttl")
            input.add(responseReader.parseTurtleFile("rdf-data/input-graphs/dataset_extension.ttl"))

            val result = deductionService.reason(input, CatalogType.DATASETS)
            val expected = responseReader.parseTurtleFile("rdf-data/expected/deduction-data/dataset_fdk_booleans.ttl")

            assertTrue(result.isIsomorphicWith(expected))
        }

        @Test
        fun `test recognize datasets described by mobilityDCAT as related to transportPortal`() {
            val input = responseReader.parseTurtleFile("rdf-data/input-graphs/dataset.ttl")
            input.add(responseReader.parseTurtleFile("rdf-data/input-graphs/mobility_dcat_extension.ttl"))

            val result = deductionService.reason(input, CatalogType.DATASETS)
            val expected = responseReader.parseTurtleFile("rdf-data/expected/deduction-data/dataset_is_transport_portal.ttl")

            assertTrue(result.isIsomorphicWith(expected))
        }
    }

    @Nested
    internal inner class InformationModel {
        @Test
        fun `test no added triples when no rules are applicable`() {
            val input = responseReader.parseTurtleFile("rdf-data/input-graphs/information_model.ttl")
            val result = deductionService.reason(input, CatalogType.INFORMATIONMODELS)
            val expected = responseReader.parseTurtleFile("rdf-data/expected/empty_graph.ttl")

            assertTrue(result.isIsomorphicWith(expected))
        }

        @Test
        fun `test add publisher rule`() {
            val input = responseReader.parseTurtleFile("rdf-data/input-graphs/information_model.ttl")
            input.add(
                input.getResource("http://test.no/catalogs/TestKatalog"),
                DCTerms.publisher,
                input.createResource("https://data.brreg.no/enhetsregisteret/oppslag/enheter/987654321")
            )

            val result = deductionService.reason(input, CatalogType.INFORMATIONMODELS)
            val expected = responseReader.parseTurtleFile("rdf-data/expected/deduction-data/information_model.ttl")

            assertTrue(result.isIsomorphicWith(expected))
        }
    }

    @Nested
    internal inner class Service {
        @Test
        fun `test no added triples when no rules are applicable`() {
            val input = responseReader.parseTurtleFile("rdf-data/input-graphs/service.ttl")
            val result = deductionService.reason(input, CatalogType.PUBLICSERVICES)
            val expected = responseReader.parseTurtleFile("rdf-data/expected/empty_graph.ttl")

            assertTrue(result.isIsomorphicWith(expected))
        }

        @Test
        fun `test add hasParticipate for participating agent`() {
            val input = responseReader.parseTurtleFile("rdf-data/input-graphs/service.ttl")
            input.add(responseReader.parseTurtleFile("rdf-data/input-graphs/service_extension.ttl"))

            val result = deductionService.reason(input, CatalogType.PUBLICSERVICES)
            val expected = responseReader.parseTurtleFile("rdf-data/expected/deduction-data/service.ttl")

            assertTrue(result.isIsomorphicWith(expected))
        }
    }
}