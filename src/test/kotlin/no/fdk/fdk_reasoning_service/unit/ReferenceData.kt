package no.fdk.fdk_reasoning_service.unit

import io.mockk.every
import io.mockk.mockk
import no.fdk.fdk_reasoning_service.cache.ReferenceDataCache
import no.fdk.fdk_reasoning_service.model.CatalogType
import no.fdk.fdk_reasoning_service.service.ReferenceDataService
import no.fdk.fdk_reasoning_service.utils.TestResponseReader
import org.apache.jena.rdf.model.ResourceFactory
import org.apache.jena.vocabulary.DCAT
import org.apache.jena.vocabulary.DCTerms
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

@Tag("unit")
class ReferenceData {
    private val referenceDataCache: ReferenceDataCache = mockk()
    private val referenceDataService = ReferenceDataService(referenceDataCache)
    private val responseReader = TestResponseReader()

    init { setupReferenceDataCacheMocks() }

    private fun setupReferenceDataCacheMocks() {
        every { referenceDataCache.conceptStatuses() } returns responseReader
            .parseTurtleFile("rdf-data/reference-data/concept_statuses.ttl")
        every { referenceDataCache.conceptSubjects() } returns responseReader
            .parseTurtleFile("rdf-data/reference-data/concept_subjects.ttl")
        every { referenceDataCache.ianaMediaTypes() } returns responseReader
            .parseTurtleFile("rdf-data/reference-data/media_types.ttl")
        every { referenceDataCache.fileTypes() } returns responseReader
            .parseTurtleFile("rdf-data/reference-data/file_types.ttl")
        every { referenceDataCache.openLicenses() } returns responseReader
            .parseTurtleFile("rdf-data/reference-data/open_licenses.ttl")
        every { referenceDataCache.linguisticSystems() } returns responseReader
            .parseTurtleFile("rdf-data/reference-data/linguistic_systems.ttl")
        every { referenceDataCache.locations() } returns responseReader
            .parseTurtleFile("rdf-data/reference-data/administrative_enheter.ttl")
        every { referenceDataCache.accessRights() } returns responseReader
            .parseTurtleFile("rdf-data/reference-data/access_rights.ttl")
        every { referenceDataCache.frequencies() } returns responseReader
            .parseTurtleFile("rdf-data/reference-data/frequencies.ttl")
        every { referenceDataCache.provenance() } returns responseReader
            .parseTurtleFile("rdf-data/reference-data/provenance_statements.ttl")
        every { referenceDataCache.publisherTypes() } returns responseReader
            .parseTurtleFile("rdf-data/reference-data/publisher_types.ttl")
        every { referenceDataCache.roleTypes() } returns responseReader
            .parseTurtleFile("rdf-data/reference-data/role_types.ttl")
        every { referenceDataCache.evidenceTypes() } returns responseReader
            .parseTurtleFile("rdf-data/reference-data/evidence_types.ttl")
        every { referenceDataCache.channelTypes() } returns responseReader
            .parseTurtleFile("rdf-data/reference-data/channel_types.ttl")
        every { referenceDataCache.mainActivities() } returns responseReader
            .parseTurtleFile("rdf-data/reference-data/main_activities.ttl")
        every { referenceDataCache.admsStatuses() } returns responseReader
            .parseTurtleFile("rdf-data/reference-data/adms_statuses.ttl")
        every { referenceDataCache.weekDays() } returns responseReader
            .parseTurtleFile("rdf-data/reference-data/week_days.ttl")
        every { referenceDataCache.datasetTypes() } returns responseReader
            .parseTurtleFile("rdf-data/reference-data/dataset-types.ttl")
        every { referenceDataCache.distributionStatuses() } returns responseReader
            .parseTurtleFile("rdf-data/reference-data/distribution_statuses.ttl")
        every { referenceDataCache.mobilityDataStandards() } returns responseReader
            .parseTurtleFile("rdf-data/reference-data/mobility_data_standards.ttl")
        every { referenceDataCache.mobilityConditions() } returns responseReader
            .parseTurtleFile("rdf-data/reference-data/mobility_conditions.ttl")
    }

    @Nested
    internal inner class Concept {
        private val conceptURI = "http://begrepskatalogen/begrep/46f4d710-4c6c-11e8-bb3e-005056821322"

        @Test
        fun `test no extra triples are added from reference data when not present as object in input`() {
            val input = responseReader.parseTurtleFile("rdf-data/input-graphs/concept.ttl")
            val result = referenceDataService.reason(input, CatalogType.CONCEPTS)
            val expected = responseReader.parseTurtleFile("rdf-data/expected/empty_graph.ttl")

            assertTrue(result.isIsomorphicWith(expected))
        }

        @Test
        fun `test concept status and concept subject triples are added from reference data`() {
            val input = responseReader.parseTurtleFile("rdf-data/input-graphs/concept.ttl")
            input.add(
                input.getResource(conceptURI),
                DCTerms.subject,
                input.createResource("https://catalog-admin-service.staging.fellesdatakatalog.digdir.no/123456789/concepts/subjects#3")
            )
            input.add(
                input.getResource(conceptURI),
                ResourceFactory.createProperty("http://publications.europa.eu/ontology/euvoc#status"),
                input.createResource("http://publications.europa.eu/resource/authority/concept-status/CURRENT")
            )

            val result = referenceDataService.reason(input, CatalogType.CONCEPTS)
            val expected = responseReader.parseTurtleFile("rdf-data/expected/reference-data/concept.ttl")

            assertTrue(result.isIsomorphicWith(expected))
        }
    }

    @Nested
    internal inner class DataService {
        private val dataServiceURI = "https://dataservice-catalog.staging.fellesdatakatalog.digdir.no/data-services/5f48b38626087749e9be175e"

        @Test
        fun `test no extra triples are added from reference data when not present as object in input`() {
            val input = responseReader.parseTurtleFile("rdf-data/input-graphs/data_service.ttl")
            val result = referenceDataService.reason(input, CatalogType.DATASERVICES)
            val expected = responseReader.parseTurtleFile("rdf-data/expected/empty_graph.ttl")

            assertTrue(result.isIsomorphicWith(expected))
        }

        @Test
        fun `test mediaTypes and fileTypes are added from reference data`() {
            val input = responseReader.parseTurtleFile("rdf-data/input-graphs/data_service.ttl")
            input.add(input.getResource(dataServiceURI), DCTerms.format, input.createResource("http://publications.europa.eu/resource/authority/file-type/XML"))

            val result = referenceDataService.reason(input, CatalogType.DATASERVICES)
            val expected = responseReader.parseTurtleFile("rdf-data/expected/reference-data/data_service.ttl")

            assertTrue(result.isIsomorphicWith(expected))
        }
    }


    @Nested
    internal inner class Dataset {
        val datasetURI = "https://dataservice-catalog.staging.fellesdatakatalog.digdir.no/data-services/5f48b38626087749e9be175e"

        @Test
        fun `test no extra triples are added from reference data when not present as object in input`() {
            val input = responseReader.parseTurtleFile("rdf-data/input-graphs/dataset.ttl")

            val result = referenceDataService.reason(input, CatalogType.DATASETS)
            val expected = responseReader.parseTurtleFile("rdf-data/expected/empty_graph.ttl")

            assertTrue(result.isIsomorphicWith(expected))
        }

        @Test
        fun `test mediaTypes and fileTypes are added from reference data`() {
            val input = responseReader.parseTurtleFile("rdf-data/input-graphs/dataset.ttl")

            val distribution = input.createResource()
            distribution.addProperty(DCTerms.format, input.createResource("http://publications.europa.eu/resource/authority/file-type/XML"))
            distribution.addProperty(DCAT.mediaType, input.createResource("https://www.iana.org/assignments/media-types/application/xml"))
            input.add(input.getResource(datasetURI), DCAT.distribution, distribution)

            val result = referenceDataService.reason(input, CatalogType.DATASETS)
            val expected = responseReader.parseTurtleFile("rdf-data/expected/reference-data/dataset_formats.ttl")

            assertTrue(result.isIsomorphicWith(expected))
        }

        @Test
        fun `test licenses, linguistic systems, locations, access rights, frequencies and provenance are added from reference data`() {
            val input = responseReader.parseTurtleFile("rdf-data/input-graphs/dataset.ttl")
            input.add(responseReader.parseTurtleFile("rdf-data/input-graphs/dataset_extension.ttl"))

            val result = referenceDataService.reason(input, CatalogType.DATASETS)
            val expected = responseReader.parseTurtleFile("rdf-data/expected/reference-data/dataset.ttl")

            assertTrue(result.isIsomorphicWith(expected))
        }

        @Test
        fun `test distribution statuses, mobility data standards and mobility conditions are added from reference data`() {
            val input = responseReader.parseTurtleFile("rdf-data/input-graphs/dataset.ttl")
            val datasetResource = input.getResource(datasetURI)

            input.add(
                datasetResource,
                ResourceFactory.createProperty("http://www.w3.org/ns/adms#status"),
                ResourceFactory.createProperty("http://publications.europa.eu/resource/authority/distribution-status/COMPLETED")
            )

            input.add(
                datasetResource,
                ResourceFactory.createProperty("https://w3id.org/mobilitydcat-ap#mobilityDataStandard"),
                ResourceFactory.createProperty("https://w3id.org/mobilitydcat-ap/mobility-data-standard/siri")
            )

            val rightsResource = input.createResource()
            rightsResource.addProperty(DCTerms.type, input.createResource("https://w3id.org/mobilitydcat-ap/conditions-for-access-and-usage/royalty-free"))
            input.add(datasetResource, DCTerms.rights, rightsResource)

            val result = referenceDataService.reason(input, CatalogType.DATASETS)
            val expected = responseReader.parseTurtleFile("rdf-data/expected/reference-data/mobility_dataset.ttl")

            assertTrue(result.isIsomorphicWith(expected))
        }
    }

    @Nested
    internal inner class InformationModel {
        private val infoModelURI = "http://test.no/catalogs/TestModell"

        @Test
        fun `test no extra triples are added from reference data when not present as object in input`() {
            val input = responseReader.parseTurtleFile("rdf-data/input-graphs/information_model.ttl")
            val result = referenceDataService.reason(input, CatalogType.INFORMATIONMODELS)
            val expected = responseReader.parseTurtleFile("rdf-data/expected/empty_graph.ttl")

            assertTrue(result.isIsomorphicWith(expected))
        }

        @Test
        fun `test licenses, linguistic systems and locations are added from reference data`() {
            val input = responseReader.parseTurtleFile("rdf-data/input-graphs/information_model.ttl")
            input.add(input.getResource(infoModelURI), DCTerms.language, input.createResource("http://publications.europa.eu/resource/authority/language/SMI"))
            input.add(input.getResource(infoModelURI), DCTerms.language, input.createResource("http://publications.europa.eu/resource/authority/language/NOB"))
            input.add(input.getResource(infoModelURI), DCTerms.license, input.createResource("http://publications.europa.eu/resource/authority/licence/CC_BY_4_0"))
            input.add(input.getResource(infoModelURI), DCTerms.spatial, input.createResource("https://data.geonorge.no/administrativeEnheter/nasjon/id/173163"))

            val result = referenceDataService.reason(input, CatalogType.INFORMATIONMODELS)
            val expected = responseReader.parseTurtleFile("rdf-data/expected/reference-data/information_model.ttl")

            assertTrue(result.isIsomorphicWith(expected))
        }
    }

    @Nested
    internal inner class Service {
        @Test
        fun `test no extra triples are added from reference data when not present as object in input`() {
            val input = responseReader.parseTurtleFile("rdf-data/input-graphs/service.ttl")
            val result = referenceDataService.reason(input, CatalogType.PUBLICSERVICES)
            val expected = responseReader.parseTurtleFile("rdf-data/expected/empty_graph.ttl")

            assertTrue(result.isIsomorphicWith(expected))
        }

        @Test
        fun `test service containing reference data triples`() {
            val input = responseReader.parseTurtleFile("rdf-data/input-graphs/service.ttl")
            input.add(responseReader.parseTurtleFile("rdf-data/input-graphs/service_extension.ttl"))

            val result = referenceDataService.reason(input, CatalogType.PUBLICSERVICES)
            val expected = responseReader.parseTurtleFile("rdf-data/expected/reference-data/service.ttl")

            assertTrue(result.isIsomorphicWith(expected))
        }
    }
}
