package no.fdk.fdk_reasoning_service.unit

import io.mockk.every
import io.mockk.mockk
import no.fdk.fdk_reasoning_service.cache.ReferenceDataCache
import no.fdk.fdk_reasoning_service.config.ApplicationURI
import no.fdk.fdk_reasoning_service.model.CatalogType
import no.fdk.fdk_reasoning_service.service.OrganizationCatalogAdapter
import no.fdk.fdk_reasoning_service.service.OrganizationService
import no.fdk.fdk_reasoning_service.utils.TestResponseReader
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
        @Test
        fun `test add triples for concept publisher`() {
            val result = orgService.reason(
                responseReader.parseTurtleFile("rdf-data/input-graphs/concept_0.ttl"),
                CatalogType.CONCEPTS,
            )
            val expected = responseReader.parseTurtleFile("rdf-data/expected/org-data/concept_0_org.ttl")

            assertTrue(result.isIsomorphicWith(expected))
        }

        @Test
        fun `test add triples for concept creator`() {
            val result = orgService.reason(
                responseReader.parseTurtleFile("rdf-data/input-graphs/concept_1.ttl"),
                CatalogType.CONCEPTS,
            )
            val expected = responseReader.parseTurtleFile("rdf-data/expected/org-data/concept_1_org.ttl")

            assertTrue(result.isIsomorphicWith(expected))
        }
    }

    @Nested
    internal inner class DataService {
        @Test
        fun `test no added triples for org with both name and orgPath`() {
            val result = orgService.reason(
                responseReader.parseTurtleFile("rdf-data/input-graphs/data_service_0.ttl"),
                CatalogType.DATASERVICES,
            )
            val expected = responseReader.parseTurtleFile("rdf-data/expected/empty_graph.ttl")

            assertTrue(result.isIsomorphicWith(expected))
        }

        @Test
        fun `test add triples for organization new in organization catalog`() {
            val result = orgService.reason(
                responseReader.parseTurtleFile("rdf-data/input-graphs/data_service_1.ttl"),
                CatalogType.DATASERVICES,
            )
            val expected = responseReader.parseTurtleFile("rdf-data/expected/org-data/data_service_1_org.ttl")

            assertTrue(result.isIsomorphicWith(expected))
        }
    }

    @Nested
    internal inner class Dataset {
        @Test
        fun `test add triples for dataset publisher`() {
            val result = orgService.reason(
                responseReader.parseTurtleFile("rdf-data/input-graphs/dataset_0.ttl"),
                CatalogType.DATASETS,
            )
            val expected = responseReader.parseTurtleFile("rdf-data/expected/org-data/dataset_0_org.ttl")

            assertTrue(result.isIsomorphicWith(expected))
        }
    }

    @Nested
    internal inner class Event {
        @Test
        fun `test add triples for event catalog publisher`() {
            val result = orgService.reason(
                responseReader.parseTurtleFile("rdf-data/input-graphs/event_0.ttl"),
                CatalogType.EVENTS,
            )
            val expected = responseReader.parseTurtleFile("rdf-data/expected/org-data/event_0_org.ttl")

            assertTrue(result.isIsomorphicWith(expected))
        }
    }

    @Nested
    internal inner class InformationModel {
        @Test
        fun `test add triples for information model publisher`() {
            val result = orgService.reason(
                responseReader.parseTurtleFile("rdf-data/input-graphs/information_model_0.ttl"),
                CatalogType.INFORMATIONMODELS,
            )
            val expected = responseReader.parseTurtleFile("rdf-data/expected/org-data/information_model_0_org.ttl")

            assertTrue(result.isIsomorphicWith(expected))
        }
    }

    @Nested
    internal inner class Service {
        @Test
        fun `test add triples for service hasCompetentAuthority`() {
            val result = orgService.reason(
                responseReader.parseTurtleFile("rdf-data/input-graphs/service_0.ttl"),
                CatalogType.PUBLICSERVICES,
            )
            val expected = responseReader.parseTurtleFile("rdf-data/expected/org-data/service_0_org.ttl")

            assertTrue(result.isIsomorphicWith(expected))
        }

        @Test
        fun `test add triples for service ownedBy`() {
            val result = orgService.reason(
                responseReader.parseTurtleFile("rdf-data/input-graphs/service_1.ttl"),
                CatalogType.PUBLICSERVICES,
            )
            val expected = responseReader.parseTurtleFile("rdf-data/expected/org-data/service_1_org.ttl")

            assertTrue(result.isIsomorphicWith(expected))
        }

        @Test
        fun `test add triples for service catalog publisher`() {
            val result = orgService.reason(
                responseReader.parseTurtleFile("rdf-data/input-graphs/service_3.ttl"),
                CatalogType.PUBLICSERVICES,
            )
            val expected = responseReader.parseTurtleFile("rdf-data/expected/org-data/service_3_org.ttl")

            assertTrue(result.isIsomorphicWith(expected))
        }
    }

}