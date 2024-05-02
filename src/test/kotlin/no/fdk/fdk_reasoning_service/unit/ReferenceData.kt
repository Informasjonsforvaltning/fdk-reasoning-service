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

    init {
        setupReferenceDataCacheMocks()
    }

    private fun setupReferenceDataCacheMocks() {
        whenever(referenceDataCache.conceptStatuses())
            .thenReturn(responseReader.parseTurtleFile("rdf-data/reference-data/concept_statuses.ttl"))
        whenever(referenceDataCache.conceptSubjects())
            .thenReturn(responseReader.parseTurtleFile("rdf-data/reference-data/concept_subjects.ttl"))
        whenever(referenceDataCache.los())
            .thenReturn(responseReader.parseTurtleFile("rdf-data/reference-data/los.ttl"))
        whenever(referenceDataCache.eurovocs())
            .thenReturn(responseReader.parseTurtleFile("rdf-data/reference-data/eurovocs.ttl"))
        whenever(referenceDataCache.dataThemes())
            .thenReturn(responseReader.parseTurtleFile("rdf-data/reference-data/data_themes.ttl"))
        whenever(referenceDataCache.ianaMediaTypes())
            .thenReturn(responseReader.parseTurtleFile("rdf-data/reference-data/media_types.ttl"))
        whenever(referenceDataCache.fileTypes())
            .thenReturn(responseReader.parseTurtleFile("rdf-data/reference-data/file_types.ttl"))
        whenever(referenceDataCache.openLicenses())
            .thenReturn(responseReader.parseTurtleFile("rdf-data/reference-data/open_licenses.ttl"))
        whenever(referenceDataCache.linguisticSystems())
            .thenReturn(responseReader.parseTurtleFile("rdf-data/reference-data/linguistic_systems.ttl"))
        whenever(referenceDataCache.locations())
            .thenReturn(
                responseReader.parseTurtleFiles(
                    listOf(
                        "rdf-data/reference-data/kommuner.ttl",
                        "rdf-data/reference-data/fylker.ttl",
                        "rdf-data/reference-data/nasjoner.ttl"
                    )
                )
            )
        whenever(referenceDataCache.accessRights())
            .thenReturn(responseReader.parseTurtleFile("rdf-data/reference-data/access_rights.ttl"))
        whenever(referenceDataCache.frequencies())
            .thenReturn(responseReader.parseTurtleFile("rdf-data/reference-data/frequencies.ttl"))
        whenever(referenceDataCache.provenance())
            .thenReturn(responseReader.parseTurtleFile("rdf-data/reference-data/provenance_statements.ttl"))
        whenever(referenceDataCache.publisherTypes())
            .thenReturn(responseReader.parseTurtleFile("rdf-data/reference-data/publisher_types.ttl"))
        whenever(referenceDataCache.roleTypes())
            .thenReturn(responseReader.parseTurtleFile("rdf-data/reference-data/role_types.ttl"))
        whenever(referenceDataCache.evidenceTypes())
            .thenReturn(responseReader.parseTurtleFile("rdf-data/reference-data/evidence_types.ttl"))
        whenever(referenceDataCache.channelTypes())
            .thenReturn(responseReader.parseTurtleFile("rdf-data/reference-data/channel_types.ttl"))
        whenever(referenceDataCache.mainActivities())
            .thenReturn(responseReader.parseTurtleFile("rdf-data/reference-data/main_activities.ttl"))
        whenever(referenceDataCache.admsStatuses())
            .thenReturn(responseReader.parseTurtleFile("rdf-data/reference-data/adms_statuses.ttl"))
        whenever(referenceDataCache.weekDays())
            .thenReturn(responseReader.parseTurtleFile("rdf-data/reference-data/week_days.ttl"))
    }

    @Nested
    internal inner class Concept {
        @Test
        fun `test no extra triples are added from reference data when not present as object in input`() {
            val result = referenceDataService.referenceDataModel(
                responseReader.parseTurtleFile("rdf-data/input-graphs/concept_0.ttl"),
                CatalogType.CONCEPTS,
            )
            val expected = responseReader.parseTurtleFile("rdf-data/expected/empty_graph.ttl")

            assertTrue(result.isIsomorphicWith(expected))
        }

        @Test
        fun `test concept status and concept subject triples are added from reference data`() {
            val result = referenceDataService.referenceDataModel(
                responseReader.parseTurtleFile("rdf-data/input-graphs/concept_1.ttl"),
                CatalogType.CONCEPTS,
            )
            val expected = responseReader.parseTurtleFile("rdf-data/expected/reference-data/concept_1_reference_data.ttl")

            assertTrue(result.isIsomorphicWith(expected))
        }
    }

    @Nested
    internal inner class DataService {

        @Test
        fun `test no extra triples are added from reference data when not present as object in input`() {
            val result = referenceDataService.referenceDataModel(
                responseReader.parseTurtleFile("rdf-data/input-graphs/data_service_0.ttl"),
                CatalogType.DATASERVICES,
            )
            val expected = responseReader.parseTurtleFile("rdf-data/expected/empty_graph.ttl")

            assertTrue(result.isIsomorphicWith(expected))
        }

        @Test
        fun `test mediaTypes and fileTypes are added from reference data`() {
            val result = referenceDataService.referenceDataModel(
                responseReader.parseTurtleFile("rdf-data/input-graphs/data_service_1.ttl"),
                CatalogType.DATASERVICES,
            )
            val expected = responseReader.parseTurtleFile("rdf-data/expected/reference-data/data_service_1_reference_data.ttl")

            assertTrue(result.isIsomorphicWith(expected))
        }
    }


    @Nested
    internal inner class Dataset {
        @Test
        fun `test no extra triples are added from reference data when not present as object in input`() {
            val result = referenceDataService.referenceDataModel(
                responseReader.parseTurtleFile("rdf-data/input-graphs/dataset_0.ttl"),
                CatalogType.DATASETS,
            )
            val expected = responseReader.parseTurtleFile("rdf-data/expected/empty_graph.ttl")

            assertTrue(result.isIsomorphicWith(expected))
        }

        @Test
        fun `test selected theme triples are added from reference data`() {
            val result = referenceDataService.referenceDataModel(
                responseReader.parseTurtleFile("rdf-data/input-graphs/dataset_1.ttl"),
                CatalogType.DATASETS,
            )
            val expected = responseReader.parseTurtleFile("rdf-data/expected/reference-data/dataset_1_reference_data.ttl")

            assertTrue(result.isIsomorphicWith(expected))
        }

        @Test
        fun `test mediaTypes and fileTypes are added from reference data`() {
            val result = referenceDataService.referenceDataModel(
                responseReader.parseTurtleFile("rdf-data/input-graphs/dataset_2.ttl"),
                CatalogType.DATASETS,
            )
            val expected = responseReader.parseTurtleFile("rdf-data/expected/reference-data/dataset_2_reference_data.ttl")

            assertTrue(result.isIsomorphicWith(expected))
        }

        @Test
        fun `test licenses, linguistic systems, locations, access rights, frequencies and provenance are added from reference data`() {
            val result = referenceDataService.referenceDataModel(
                responseReader.parseTurtleFile("rdf-data/input-graphs/dataset_3.ttl"),
                CatalogType.DATASETS,
            )
            val expected = responseReader.parseTurtleFile("rdf-data/expected/reference-data/dataset_3_reference_data.ttl")

            assertTrue(result.isIsomorphicWith(expected))
        }
    }

    @Nested
    internal inner class InformationModel {
        @Test
        fun `test no extra triples are added from reference data when not present as object in input`() {
            val result = referenceDataService.referenceDataModel(
                responseReader.parseTurtleFile("rdf-data/input-graphs/information_model_0.ttl"),
                CatalogType.INFORMATIONMODELS,
            )
            val expected = responseReader.parseTurtleFile("rdf-data/expected/empty_graph.ttl")

            assertTrue(result.isIsomorphicWith(expected))
        }

        @Test
        fun `test selected theme triples are added from reference data`() {
            val result = referenceDataService.referenceDataModel(
                responseReader.parseTurtleFile("rdf-data/input-graphs/information_model_1.ttl"),
                CatalogType.INFORMATIONMODELS,
            )
            val expected = responseReader.parseTurtleFile("rdf-data/expected/reference-data/information_model_1_reference_data.ttl")

            assertTrue(result.isIsomorphicWith(expected))
        }

        @Test
        fun `test licenses, linguistic systems and locations are added from reference data`() {
            val result = referenceDataService.referenceDataModel(
                responseReader.parseTurtleFile("rdf-data/input-graphs/information_model_2.ttl"),
                CatalogType.INFORMATIONMODELS,
            )
            val expected = responseReader.parseTurtleFile("rdf-data/expected/reference-data/information_model_2_reference_data.ttl")

            assertTrue(result.isIsomorphicWith(expected))
        }
    }

    @Nested
    internal inner class Service {
        @Test
        fun `test no extra triples are added from reference data when not present as object in input`() {
            val result = referenceDataService.referenceDataModel(
                responseReader.parseTurtleFile("rdf-data/input-graphs/service_0.ttl"),
                CatalogType.PUBLICSERVICES,
            )
            val expected = responseReader.parseTurtleFile("rdf-data/expected/empty_graph.ttl")

            assertTrue(result.isIsomorphicWith(expected))
        }

        @Test
        fun `test selected theme triples are added from reference data`() {
            val result = referenceDataService.referenceDataModel(
                responseReader.parseTurtleFile("rdf-data/input-graphs/service_1.ttl"),
                CatalogType.PUBLICSERVICES,
            )
            val expected = responseReader.parseTurtleFile("rdf-data/expected/reference-data/service_1_reference_data.ttl")

            assertTrue(result.isIsomorphicWith(expected))
        }

        @Test
        fun `test publisher types, adms statuses, role types, evidence types, channel types and opening hours (weekdays) are added from reference data`() {
            val result = referenceDataService.referenceDataModel(
                responseReader.parseTurtleFile("rdf-data/input-graphs/service_2.ttl"),
                CatalogType.PUBLICSERVICES,
            )
            val expected = responseReader.parseTurtleFile("rdf-data/expected/reference-data/service_2_reference_data.ttl")

            assertTrue(result.isIsomorphicWith(expected))
        }

        @Test
        fun `test linguistic systems and main activities are added from reference data`() {
            val result = referenceDataService.referenceDataModel(
                responseReader.parseTurtleFile("rdf-data/input-graphs/service_3.ttl"),
                CatalogType.PUBLICSERVICES,
            )
            val expected = responseReader.parseTurtleFile("rdf-data/expected/reference-data/service_3_reference_data.ttl")

            assertTrue(result.isIsomorphicWith(expected))
        }
    }
}
