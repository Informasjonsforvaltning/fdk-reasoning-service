package no.fdk.reasoning.unit

import io.mockk.every
import io.mockk.mockk
import no.fdk.reasoning.cache.ReferenceDataCache
import no.fdk.reasoning.model.CatalogType
import no.fdk.reasoning.service.ReferenceDataService
import no.fdk.reasoning.utils.TestResponseReader
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.RDFNode
import org.apache.jena.rdf.model.ResourceFactory
import org.apache.jena.vocabulary.DCAT
import org.apache.jena.vocabulary.DCTerms
import org.apache.jena.vocabulary.DC_11
import org.apache.jena.vocabulary.SKOS
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@Tag("unit")
class ReferenceData {
    private val referenceDataCache: ReferenceDataCache = mockk()
    private val referenceDataService = ReferenceDataService(referenceDataCache)
    private val responseReader = TestResponseReader()

    init {
        setupReferenceDataCacheMocks()
    }

    private fun setupReferenceDataCacheMocks() {
        every { referenceDataCache.conceptStatuses() } returns
            responseReader
                .parseTurtleFile("rdf-data/reference-data/concept_statuses.ttl")
        every { referenceDataCache.conceptSubjects() } returns
            responseReader
                .parseTurtleFile("rdf-data/reference-data/concept_subjects.ttl")
        every { referenceDataCache.ianaMediaTypes() } returns
            responseReader
                .parseTurtleFile("rdf-data/reference-data/media_types.ttl")
        every { referenceDataCache.fileTypes() } returns
            responseReader
                .parseTurtleFile("rdf-data/reference-data/file_types.ttl")
        every { referenceDataCache.licences() } returns
            responseReader
                .parseTurtleFile("rdf-data/reference-data/licences.ttl")
        every { referenceDataCache.languages() } returns
            responseReader
                .parseTurtleFile("rdf-data/reference-data/languages.ttl")
        every { referenceDataCache.locations() } returns
            responseReader
                .parseTurtleFile("rdf-data/reference-data/administrative_enheter.ttl")
        every { referenceDataCache.accessRights() } returns
            responseReader
                .parseTurtleFile("rdf-data/reference-data/access_rights.ttl")
        every { referenceDataCache.frequencies() } returns
            responseReader
                .parseTurtleFile("rdf-data/reference-data/frequencies.ttl")
        every { referenceDataCache.provenance() } returns
            responseReader
                .parseTurtleFile("rdf-data/reference-data/provenance_statements.ttl")
        every { referenceDataCache.publisherTypes() } returns
            responseReader
                .parseTurtleFile("rdf-data/reference-data/publisher_types.ttl")
        every { referenceDataCache.roleTypes() } returns
            responseReader
                .parseTurtleFile("rdf-data/reference-data/role_types.ttl")
        every { referenceDataCache.evidenceTypes() } returns
            responseReader
                .parseTurtleFile("rdf-data/reference-data/evidence_types.ttl")
        every { referenceDataCache.channelTypes() } returns
            responseReader
                .parseTurtleFile("rdf-data/reference-data/channel_types.ttl")
        every { referenceDataCache.mainActivities() } returns
            responseReader
                .parseTurtleFile("rdf-data/reference-data/main_activities.ttl")
        every { referenceDataCache.admsStatuses() } returns
            responseReader
                .parseTurtleFile("rdf-data/reference-data/adms_statuses.ttl")
        every { referenceDataCache.weekDays() } returns
            responseReader
                .parseTurtleFile("rdf-data/reference-data/week_days.ttl")
        every { referenceDataCache.datasetTypes() } returns
            responseReader
                .parseTurtleFile("rdf-data/reference-data/dataset-types.ttl")
        every { referenceDataCache.distributionStatuses() } returns
            responseReader
                .parseTurtleFile("rdf-data/reference-data/distribution_statuses.ttl")
        every { referenceDataCache.plannedAvailabilities() } returns
            responseReader
                .parseTurtleFile("rdf-data/reference-data/planned_availabilities.ttl")
        every { referenceDataCache.mobilityDataStandards() } returns
            responseReader
                .parseTurtleFile("rdf-data/reference-data/mobility_data_standards.ttl")
        every { referenceDataCache.mobilityConditions() } returns
            responseReader
                .parseTurtleFile("rdf-data/reference-data/mobility_conditions.ttl")
        every { referenceDataCache.highValueCategories() } returns
            responseReader
                .parseTurtleFile("rdf-data/reference-data/high-value-categories.ttl")
        every { referenceDataCache.qualityDimensions() } returns
            responseReader
                .parseTurtleFile("rdf-data/reference-data/quality-dimension.ttl")
        every { referenceDataCache.legalResourceTypes() } returns
            responseReader
                .parseTurtleFile("rdf-data/reference-data/legal-resource-type.ttl")
        every { referenceDataCache.geonames() } returns
            responseReader
                .parseTurtleFile("rdf-data/reference-data/geonames.ttl")
        every { referenceDataCache.euContinents() } returns
            responseReader
                .parseTurtleFile("rdf-data/reference-data/eu_continents.ttl")
        every { referenceDataCache.euCountries() } returns
            responseReader
                .parseTurtleFile("rdf-data/reference-data/eu_countries.ttl")
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
                input.createResource("https://catalog-admin-service.staging.fellesdatakatalog.digdir.no/123456789/concepts/subjects#3"),
            )
            input.add(
                input.getResource(conceptURI),
                ResourceFactory.createProperty("http://publications.europa.eu/ontology/euvoc#status"),
                input.createResource("http://publications.europa.eu/resource/authority/concept-status/CURRENT"),
            )

            val result = referenceDataService.reason(input, CatalogType.CONCEPTS)
            val expected = responseReader.parseTurtleFile("rdf-data/expected/reference-data/concept.ttl")

            assertTrue(result.isIsomorphicWith(expected))
        }
    }

    @Nested
    internal inner class DataService {
        private val dataServiceURI =
            "https://dataservice-catalog.staging.fellesdatakatalog.digdir.no/data-services/5f48b38626087749e9be175e"

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
            input.add(
                input.getResource(dataServiceURI),
                DCTerms.format,
                input.createResource("http://publications.europa.eu/resource/authority/file-type/XML"),
            )

            val result = referenceDataService.reason(input, CatalogType.DATASERVICES)
            val expected = responseReader.parseTurtleFile("rdf-data/expected/reference-data/data_service.ttl")

            assertTrue(result.isIsomorphicWith(expected))
        }

        @Test
        fun `test open licenses are added from reference data`() {
            val input = responseReader.parseTurtleFile("rdf-data/input-graphs/data_service.ttl")
            input.add(
                input.getResource(dataServiceURI),
                DCTerms.license,
                input.createResource("http://publications.europa.eu/resource/authority/licence/CC_BY_4_0"),
            )

            val result = referenceDataService.reason(input, CatalogType.DATASERVICES)

            assertTrue(
                result.contains(
                    ResourceFactory.createResource("http://publications.europa.eu/resource/authority/licence/CC_BY_4_0"),
                    SKOS.prefLabel,
                    "Creative Commons Attribution 4.0 International",
                    "en",
                ),
            )
        }

        @Test
        fun `test access rights are added from reference data`() {
            val input = responseReader.parseTurtleFile("rdf-data/input-graphs/data_service.ttl")
            input.add(
                input.getResource(dataServiceURI),
                DCTerms.accessRights,
                input.createResource("http://publications.europa.eu/resource/authority/access-right/PUBLIC"),
            )

            val result = referenceDataService.reason(input, CatalogType.DATASERVICES)
            val accessRight =
                ResourceFactory.createResource("http://publications.europa.eu/resource/authority/access-right/PUBLIC")

            assertTrue(
                result.contains(accessRight, DC_11.identifier, "PUBLIC"),
                "code is resolvable from reference data",
            )
            assertTrue(
                result.listStatements(accessRight, SKOS.prefLabel, null as RDFNode?).hasNext(),
                "prefLabel is resolvable from reference data",
            )
        }

        @Test
        fun `test adms status is added from reference data`() {
            val input = responseReader.parseTurtleFile("rdf-data/input-graphs/data_service.ttl")
            input.add(
                input.getResource(dataServiceURI),
                ResourceFactory.createProperty("http://www.w3.org/ns/adms#status"),
                input.createResource("http://publications.europa.eu/resource/authority/distribution-status/COMPLETED"),
            )

            val result = referenceDataService.reason(input, CatalogType.DATASERVICES)

            assertTrue(
                result.contains(
                    ResourceFactory.createResource(
                        "http://publications.europa.eu/resource/authority/distribution-status/COMPLETED",
                    ),
                    DC_11.identifier,
                    "COMPLETED",
                ),
            )
        }

        @Test
        fun `test planned availability is added from reference data`() {
            val input = responseReader.parseTurtleFile("rdf-data/input-graphs/data_service.ttl")
            input.add(
                input.getResource(dataServiceURI),
                ResourceFactory.createProperty("http://data.europa.eu/r5r/availability"),
                input.createResource("http://publications.europa.eu/resource/authority/planned-availability/STABLE"),
            )

            val result = referenceDataService.reason(input, CatalogType.DATASERVICES)

            assertTrue(
                result.contains(
                    ResourceFactory.createResource(
                        "http://publications.europa.eu/resource/authority/planned-availability/STABLE",
                    ),
                    DC_11.identifier,
                    "STABLE",
                ),
            )
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
        fun `test licenses, languages, locations, access rights, frequencies and provenance are added from reference data`() {
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
                ResourceFactory.createProperty("http://publications.europa.eu/resource/authority/distribution-status/COMPLETED"),
            )

            input.add(
                datasetResource,
                ResourceFactory.createProperty("https://w3id.org/mobilitydcat-ap#mobilityDataStandard"),
                ResourceFactory.createProperty("https://w3id.org/mobilitydcat-ap/mobility-data-standard/siri"),
            )

            val rightsResource = input.createResource()
            rightsResource.addProperty(
                DCTerms.type,
                input.createResource("https://w3id.org/mobilitydcat-ap/conditions-for-access-and-usage/royalty-free"),
            )
            input.add(datasetResource, DCTerms.rights, rightsResource)

            val result = referenceDataService.reason(input, CatalogType.DATASETS)
            val expected = responseReader.parseTurtleFile("rdf-data/expected/reference-data/mobility_dataset.ttl")

            assertTrue(result.isIsomorphicWith(expected))
        }

        @Test
        fun `test high value categories, quality dimensions and legal resource types are added from reference data`() {
            val input = responseReader.parseTurtleFile("rdf-data/input-graphs/dataset.ttl")
            val datasetResource = input.getResource(datasetURI)

            input.add(
                datasetResource,
                ResourceFactory.createProperty("http://data.europa.eu/r5r/hvdCategory"),
                input.createResource("http://data.europa.eu/bna/c_164e0bf5"),
            )

            val qualityAnnotation = input.createResource()
            qualityAnnotation.addProperty(
                ResourceFactory.createProperty("http://www.w3.org/ns/dqv#inDimension"),
                input.createResource("https://data.norge.no/vocabulary/quality-dimension#accuracy"),
            )
            input.add(datasetResource, ResourceFactory.createProperty("http://www.w3.org/ns/dqv#hasQualityAnnotation"), qualityAnnotation)

            val legalResource = input.createResource()
            legalResource.addProperty(
                DCTerms.type,
                input.createResource("https://data.norge.no/vocabulary/legal-resource-type#act"),
            )
            input.add(datasetResource, ResourceFactory.createProperty("http://data.europa.eu/r5r/applicableLegislation"), legalResource)

            val result = referenceDataService.reason(input, CatalogType.DATASETS)

            assertTrue(
                result.contains(
                    ResourceFactory.createResource("http://data.europa.eu/bna/c_164e0bf5"),
                    SKOS.prefLabel,
                    "Meteorological",
                    "en",
                ),
            )

            assertTrue(
                result.contains(
                    ResourceFactory.createResource("https://data.norge.no/vocabulary/quality-dimension#accuracy"),
                    SKOS.prefLabel,
                    "accuracy",
                    "en",
                ),
            )

            assertTrue(
                result.contains(
                    ResourceFactory.createResource("https://data.norge.no/vocabulary/legal-resource-type#act"),
                    SKOS.prefLabel,
                    "act",
                    "en",
                ),
            )
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
        fun `test licenses, languages and locations are added from reference data`() {
            val input = responseReader.parseTurtleFile("rdf-data/input-graphs/information_model.ttl")
            input.add(
                input.getResource(infoModelURI),
                DCTerms.language,
                input.createResource("http://publications.europa.eu/resource/authority/language/SMI"),
            )
            input.add(
                input.getResource(infoModelURI),
                DCTerms.language,
                input.createResource("http://publications.europa.eu/resource/authority/language/NOB"),
            )
            input.add(
                input.getResource(infoModelURI),
                DCTerms.license,
                input.createResource("http://publications.europa.eu/resource/authority/licence/CC_BY_4_0"),
            )
            input.add(
                input.getResource(infoModelURI),
                DCTerms.spatial,
                input.createResource("https://data.geonorge.no/administrativeEnheter/nasjon/id/173163"),
            )

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

        @Test
        fun `test exception is thrown when locations missing in reference data cache`() {
            every { referenceDataCache.locations() } returns ModelFactory.createDefaultModel()

            val input = responseReader.parseTurtleFile("rdf-data/input-graphs/service.ttl")

            assertFailsWith<Exception> {
                referenceDataService.reason(input, CatalogType.PUBLICSERVICES)
            }
        }

        @Test
        fun `test adds extra triples when containing relevant location`() {
            val input = responseReader.parseTurtleFile("rdf-data/input-graphs/service.ttl")
            val datasetResource = input.getResource("http://public-service-publisher.fellesdatakatalog.digdir.no/services/12")

            input.add(
                datasetResource,
                ResourceFactory.createProperty(DCTerms.spatial.uri),
                ResourceFactory.createProperty("http://sws.geonames.org/3162656/"),
            )

            val result = referenceDataService.reason(input, CatalogType.PUBLICSERVICES)
            val expected = responseReader.parseTurtleFile("rdf-data/expected/reference-data/service_location.ttl")

            assertTrue(result.isIsomorphicWith(expected))
        }
    }
}
