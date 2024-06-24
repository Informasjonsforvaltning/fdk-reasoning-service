package no.fdk.fdk_reasoning_service.unit

import io.mockk.every
import io.mockk.mockk
import no.fdk.fdk_reasoning_service.cache.ReferenceDataCache
import no.fdk.fdk_reasoning_service.config.ApplicationURI
import no.fdk.fdk_reasoning_service.model.CatalogType
import no.fdk.fdk_reasoning_service.rdf.CV
import no.fdk.fdk_reasoning_service.service.OrganizationCatalogAdapter
import no.fdk.fdk_reasoning_service.service.OrganizationService
import no.fdk.fdk_reasoning_service.utils.TestResponseReader
import org.apache.jena.sparql.vocabulary.FOAF
import org.apache.jena.vocabulary.DCTerms
import org.apache.jena.vocabulary.RDF
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

@Tag("unit")
class Organization {
    private val uris: ApplicationURI = mockk()
    private val referenceDataCache: ReferenceDataCache = mockk()
    private val orgAdapter: OrganizationCatalogAdapter = mockk()
    private val orgService = OrganizationService(referenceDataCache, uris, orgAdapter)
    private val responseReader = TestResponseReader()

    init { setupReferenceDataCacheMocks() }

    private fun setupReferenceDataCacheMocks() {
        every { referenceDataCache.organizations() } returns responseReader
            .parseTurtleFile("rdf-data/organization-catalog/orgs.ttl")

        every { orgAdapter.orgPathAdapter(any(), any()) } returns "/GENERATED/ORG/PATH"
        every { orgAdapter.downloadOrgData("http://localhost:5050/organizations/972417866") } returns responseReader
            .parseTurtleFile("rdf-data/organization-catalog/org.ttl")
            .getResource("http://localhost:5050/organizations/972417866")
        every { orgAdapter.downloadOrgData("http://localhost:5050/organizations/987654321") } returns null

        every { uris.orgExternal } returns "http://localhost:5050/organizations"
    }


    @Nested
    internal inner class Concept {
        private val conceptURI = "http://begrepskatalogen/begrep/46f4d710-4c6c-11e8-bb3e-005056821322"

        @Test
        fun `test add triples for concept publisher`() {
            val input = responseReader.parseTurtleFile("rdf-data/input-graphs/concept.ttl")
            input.add(input.getResource(conceptURI), DCTerms.publisher, input.createResource("https://data.brreg.no/enhetsregisteret/api/enheter/910298062"))

            val result = orgService.reason(input, CatalogType.CONCEPTS)
            val expected = responseReader.parseTurtleFile("rdf-data/expected/org-data/concept_publisher_org.ttl")

            assertTrue(result.isIsomorphicWith(expected))
        }

        @Test
        fun `test add triples for concept creator`() {
            val input = responseReader.parseTurtleFile("rdf-data/input-graphs/concept.ttl")
            input.add(input.getResource(conceptURI), DCTerms.creator, input.createResource("http://localhost:5050/organizations/991825827"))

            val result = orgService.reason(input, CatalogType.CONCEPTS)
            val expected = responseReader.parseTurtleFile("rdf-data/expected/org-data/concept_creator_org.ttl")

            assertTrue(result.isIsomorphicWith(expected))
        }
    }

    @Nested
    internal inner class DataService {
        private val dataServiceURI = "https://dataservice-catalog.staging.fellesdatakatalog.digdir.no/data-services/5f48b38626087749e9be175e"

        @Test
        fun `test no added triples for org with both name and orgPath`() {
            val input = responseReader.parseTurtleFile("rdf-data/input-graphs/data_service.ttl")
            val result = orgService.reason(input, CatalogType.DATASERVICES)
            val expected = responseReader.parseTurtleFile("rdf-data/expected/empty_graph.ttl")

            assertTrue(result.isIsomorphicWith(expected))
        }

        @Test
        fun `test add triples for organization new in organization catalog`() {
            val input = responseReader.parseTurtleFile("rdf-data/input-graphs/data_service.ttl")
            input.add(
                input.getResource(dataServiceURI),
                DCTerms.publisher,
                input.createResource("https://organization-catalog.staging.fellesdatakatalog.digdir.no/organizations/972417866")
            )

            val result = orgService.reason(input, CatalogType.DATASERVICES)
            val expected = responseReader.parseTurtleFile("rdf-data/expected/org-data/data_service_org.ttl")

            assertTrue(result.isIsomorphicWith(expected))
        }
    }

    @Nested
    internal inner class Dataset {
        @Test
        fun `test add triples for dataset publisher`() {
            val input = responseReader.parseTurtleFile("rdf-data/input-graphs/dataset.ttl")
            input.add(
                input.getResource("http://registration-api:8080/catalogs/910298062"),
                DCTerms.publisher,
                input.createResource("https://organization-catalog.staging.fellesdatakatalog.digdir.no/organizations/910298062")
            )

            val result = orgService.reason(input, CatalogType.DATASETS)
            val expected = responseReader.parseTurtleFile("rdf-data/expected/org-data/dataset_org.ttl")

            assertTrue(result.isIsomorphicWith(expected))
        }
    }

    @Nested
    internal inner class Event {
        @Test
        fun `test add triples for event catalog publisher`() {
            val result = orgService.reason(
                responseReader.parseTurtleFile("rdf-data/input-graphs/event.ttl"),
                CatalogType.EVENTS,
            )
            val expected = responseReader.parseTurtleFile("rdf-data/expected/org-data/event_org.ttl")

            assertTrue(result.isIsomorphicWith(expected))
        }
    }

    @Nested
    internal inner class InformationModel {
        @Test
        fun `test add triples for information model publisher`() {
            val input = responseReader.parseTurtleFile("rdf-data/input-graphs/information_model.ttl")

            val publisher = input.createResource("https://data.brreg.no/enhetsregisteret/oppslag/enheter/987654321")
            publisher.addProperty(RDF.type, FOAF.Agent)
            publisher.addProperty(FOAF.name, "Testetaten", "nb")
            input.add(input.getResource("http://test.no/catalogs/TestModell"), DCTerms.publisher, publisher)

            val result = orgService.reason(input, CatalogType.INFORMATIONMODELS)
            val expected = responseReader.parseTurtleFile("rdf-data/expected/org-data/information_model_org.ttl")

            assertTrue(result.isIsomorphicWith(expected))
        }
    }

    @Nested
    internal inner class Service {
        private val serviceURI = "http://public-service-publisher.fellesdatakatalog.digdir.no/services/12"

        @Test
        fun `test no added triples when no applicable org triples`() {
            val input = responseReader.parseTurtleFile("rdf-data/input-graphs/service.ttl")
            val result = orgService.reason(input, CatalogType.PUBLICSERVICES)
            val expected = responseReader.parseTurtleFile("rdf-data/expected/empty_graph.ttl")

            assertTrue(result.isIsomorphicWith(expected))
        }

        @Test
        fun `test add triples for service hasCompetentAuthority`() {
            val input = responseReader.parseTurtleFile("rdf-data/input-graphs/service.ttl")
            input.add(
                input.getResource(serviceURI),
                CV.hasCompetentAuthority,
                input.createResource("https://organization-catalog.fellesdatakatalog.digdir.no/organizations/910298062")
            )

            val result = orgService.reason(input, CatalogType.PUBLICSERVICES)
            val expected = responseReader.parseTurtleFile("rdf-data/expected/org-data/service_org.ttl")

            assertTrue(result.isIsomorphicWith(expected))
        }

        @Test
        fun `test add triples for service ownedBy`() {
            val input = responseReader.parseTurtleFile("rdf-data/input-graphs/service.ttl")
            input.add(
                input.getResource(serviceURI),
                CV.ownedBy,
                input.createResource("https://organization-catalog.fellesdatakatalog.digdir.no/organizations/910298062")
            )

            val result = orgService.reason(input, CatalogType.PUBLICSERVICES)
            val expected = responseReader.parseTurtleFile("rdf-data/expected/org-data/service_org.ttl")

            assertTrue(result.isIsomorphicWith(expected))
        }

        @Test
        fun `test add triples for service catalog publisher`() {
            val input = responseReader.parseTurtleFile("rdf-data/input-graphs/service.ttl")
            input.add(
                input.getResource("http://localhost:5050/fdk-public-service-publisher.ttl#GeneratedCatalog"),
                DCTerms.publisher,
                input.createResource("https://organization-catalog.fellesdatakatalog.digdir.no/organizations/910298062")
            )

            val result = orgService.reason(input, CatalogType.PUBLICSERVICES)
            val expected = responseReader.parseTurtleFile("rdf-data/expected/org-data/service_org.ttl")

            assertTrue(result.isIsomorphicWith(expected))
        }
    }

}