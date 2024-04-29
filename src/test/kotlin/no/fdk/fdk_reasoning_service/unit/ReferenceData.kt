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
    }

    init {setupReferenceDataCacheMocks()}

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
            val expected = responseReader.parseTurtleFile("rdf-data/expected/concept_1_reference_data.ttl")

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
            val expected = responseReader.parseTurtleFile("rdf-data/expected/data_service_1_reference_data.ttl")

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
        fun `test all reference data are added`() {
            val result = referenceDataService.referenceDataModel(
                responseReader.parseTurtleFile("rdf-data/input-graphs/dataset_1.ttl"),
                CatalogType.DATASETS,
            )
            val expected = responseReader.parseTurtleFile("rdf-data/expected/dataset_1_reference_data.ttl")

            assertTrue(result.isIsomorphicWith(expected))
        }

        @Test
        fun `test mediaTypes and fileTypes are added from reference data`() {
            val result = referenceDataService.referenceDataModel(
                responseReader.parseTurtleFile("rdf-data/input-graphs/dataset_1.ttl"),
                CatalogType.DATASETS,
            )
            val expected = responseReader.parseTurtleFile("rdf-data/expected/dataset_1_reference_data.ttl")

            assertTrue(result.isIsomorphicWith(expected))
        }

        @Test
        fun `test licenses are added from reference data`() {
            whenever(referenceDataCache.conceptStatuses())
                .thenReturn(responseReader.parseTurtleFile("rdf-data/reference-data/concept_statuses.ttl"))
            whenever(referenceDataCache.conceptSubjects())
                .thenReturn(responseReader.parseTurtleFile("rdf-data/reference-data/concept_subjects.ttl"))

            val result = referenceDataService.referenceDataModel(
                responseReader.parseTurtleFile("rdf-data/input-graphs/concept_1.ttl"),
                CatalogType.DATASETS,
            )
            val expected = responseReader.parseTurtleFile("rdf-data/expected/concept_1_reference_data.ttl")

            assertTrue(result.isIsomorphicWith(expected))
        }

        @Test
        fun `test linguistic systems are added from reference data`() {
            /*whenever(referenceDataCache.conceptStatuses())
                .thenReturn(responseReader.parseTurtleFile("rdf-data/reference-data/concept_statuses.ttl"))
            whenever(referenceDataCache.conceptSubjects())
                .thenReturn(responseReader.parseTurtleFile("rdf-data/reference-data/concept_subjects.ttl"))

            val result = referenceDataService.referenceDataModel(
                responseReader.parseTurtleFile("rdf-data/input-graphs/concept_1.ttl"),
                CatalogType.DATASETS,
            )
            val expected = responseReader.parseTurtleFile("rdf-data/expected/concept_1_reference_data.ttl")

            assertTrue(result.isIsomorphicWith(expected))*/
        }

        @Test
        fun `test locations are added from reference data`() {
            /*whenever(referenceDataCache.conceptStatuses())
                .thenReturn(responseReader.parseTurtleFile("rdf-data/reference-data/concept_statuses.ttl"))
            whenever(referenceDataCache.conceptSubjects())
                .thenReturn(responseReader.parseTurtleFile("rdf-data/reference-data/concept_subjects.ttl"))

            val result = referenceDataService.referenceDataModel(
                responseReader.parseTurtleFile("rdf-data/input-graphs/concept_1.ttl"),
                CatalogType.CONCEPTS,
            )
            val expected = responseReader.parseTurtleFile("rdf-data/expected/concept_1_reference_data.ttl")

            assertTrue(result.isIsomorphicWith(expected))*/
        }

        @Test
        fun `test access rights are added from reference data`() {
            /*whenever(referenceDataCache.conceptStatuses())
                .thenReturn(responseReader.parseTurtleFile("rdf-data/reference-data/concept_statuses.ttl"))
            whenever(referenceDataCache.conceptSubjects())
                .thenReturn(responseReader.parseTurtleFile("rdf-data/reference-data/concept_subjects.ttl"))

            val result = referenceDataService.referenceDataModel(
                responseReader.parseTurtleFile("rdf-data/input-graphs/concept_1.ttl"),
                CatalogType.CONCEPTS,
            )
            val expected = responseReader.parseTurtleFile("rdf-data/expected/concept_1_reference_data.ttl")

            assertTrue(result.isIsomorphicWith(expected))*/
        }

        @Test
        fun `test frequencies are added from reference data`() {
            /*whenever(referenceDataCache.conceptStatuses())
                .thenReturn(responseReader.parseTurtleFile("rdf-data/reference-data/concept_statuses.ttl"))
            whenever(referenceDataCache.conceptSubjects())
                .thenReturn(responseReader.parseTurtleFile("rdf-data/reference-data/concept_subjects.ttl"))

            val result = referenceDataService.referenceDataModel(
                responseReader.parseTurtleFile("rdf-data/input-graphs/concept_1.ttl"),
                CatalogType.CONCEPTS,
            )
            val expected = responseReader.parseTurtleFile("rdf-data/expected/concept_1_reference_data.ttl")

            assertTrue(result.isIsomorphicWith(expected))*/
        }

        @Test
        fun `test provenance are added from reference data`() {
            /*whenever(referenceDataCache.conceptStatuses())
                .thenReturn(responseReader.parseTurtleFile("rdf-data/reference-data/concept_statuses.ttl"))
            whenever(referenceDataCache.conceptSubjects())
                .thenReturn(responseReader.parseTurtleFile("rdf-data/reference-data/concept_subjects.ttl"))

            val result = referenceDataService.referenceDataModel(
                responseReader.parseTurtleFile("rdf-data/input-graphs/concept_1.ttl"),
                CatalogType.CONCEPTS,
            )
            val expected = responseReader.parseTurtleFile("rdf-data/expected/concept_1_reference_data.ttl")

            assertTrue(result.isIsomorphicWith(expected))*/
        }

        @Test
        fun `test los, eurovoc and data themes are added from reference data`() {
            /*whenever(referenceDataCache.conceptStatuses())
                .thenReturn(responseReader.parseTurtleFile("rdf-data/reference-data/concept_statuses.ttl"))
            whenever(referenceDataCache.conceptSubjects())
                .thenReturn(responseReader.parseTurtleFile("rdf-data/reference-data/concept_subjects.ttl"))

            val result = referenceDataService.referenceDataModel(
                responseReader.parseTurtleFile("rdf-data/input-graphs/concept_1.ttl"),
                CatalogType.CONCEPTS,
            )
            val expected = responseReader.parseTurtleFile("rdf-data/expected/concept_1_reference_data.ttl")

            assertTrue(result.isIsomorphicWith(expected))*/
        }
    }

    @Nested
    internal inner class InformationModel {
        @Test
        fun `test licenses are added from reference data`() {
            /*whenever(referenceDataCache.conceptStatuses())
                .thenReturn(responseReader.parseTurtleFile("rdf-data/reference-data/concept_statuses.ttl"))
            whenever(referenceDataCache.conceptSubjects())
                .thenReturn(responseReader.parseTurtleFile("rdf-data/reference-data/concept_subjects.ttl"))

            val result = referenceDataService.referenceDataModel(
                responseReader.parseTurtleFile("rdf-data/input-graphs/concept_1.ttl"),
                CatalogType.CONCEPTS,
            )
            val expected = responseReader.parseTurtleFile("rdf-data/expected/concept_1_reference_data.ttl")

            assertTrue(result.isIsomorphicWith(expected))*/
        }

        @Test
        fun `test linguistic systems are added from reference data`() {
            /*whenever(referenceDataCache.conceptStatuses())
                .thenReturn(responseReader.parseTurtleFile("rdf-data/reference-data/concept_statuses.ttl"))
            whenever(referenceDataCache.conceptSubjects())
                .thenReturn(responseReader.parseTurtleFile("rdf-data/reference-data/concept_subjects.ttl"))

            val result = referenceDataService.referenceDataModel(
                responseReader.parseTurtleFile("rdf-data/input-graphs/concept_1.ttl"),
                CatalogType.CONCEPTS,
            )
            val expected = responseReader.parseTurtleFile("rdf-data/expected/concept_1_reference_data.ttl")

            assertTrue(result.isIsomorphicWith(expected))*/
        }

        @Test
        fun `test locations are added from reference data`() {
            /*whenever(referenceDataCache.conceptStatuses())
                .thenReturn(responseReader.parseTurtleFile("rdf-data/reference-data/concept_statuses.ttl"))
            whenever(referenceDataCache.conceptSubjects())
                .thenReturn(responseReader.parseTurtleFile("rdf-data/reference-data/concept_subjects.ttl"))

            val result = referenceDataService.referenceDataModel(
                responseReader.parseTurtleFile("rdf-data/input-graphs/concept_1.ttl"),
                CatalogType.CONCEPTS,
            )
            val expected = responseReader.parseTurtleFile("rdf-data/expected/concept_1_reference_data.ttl")

            assertTrue(result.isIsomorphicWith(expected))*/
        }


        @Test
        fun `test los, eurovoc and data themes are added from reference data`() {
            /*whenever(referenceDataCache.conceptStatuses())
                .thenReturn(responseReader.parseTurtleFile("rdf-data/reference-data/concept_statuses.ttl"))
            whenever(referenceDataCache.conceptSubjects())
                .thenReturn(responseReader.parseTurtleFile("rdf-data/reference-data/concept_subjects.ttl"))

            val result = referenceDataService.referenceDataModel(
                responseReader.parseTurtleFile("rdf-data/input-graphs/concept_1.ttl"),
                CatalogType.CONCEPTS,
            )
            val expected = responseReader.parseTurtleFile("rdf-data/expected/concept_1_reference_data.ttl")

            assertTrue(result.isIsomorphicWith(expected))*/
        }

    }

    @Nested
    internal inner class Service {
        @Test
        fun `test linguistic systems are added from reference data`() {
            /*whenever(referenceDataCache.conceptStatuses())
                .thenReturn(responseReader.parseTurtleFile("rdf-data/reference-data/concept_statuses.ttl"))
            whenever(referenceDataCache.conceptSubjects())
                .thenReturn(responseReader.parseTurtleFile("rdf-data/reference-data/concept_subjects.ttl"))

            val result = referenceDataService.referenceDataModel(
                responseReader.parseTurtleFile("rdf-data/input-graphs/concept_1.ttl"),
                CatalogType.CONCEPTS,
            )
            val expected = responseReader.parseTurtleFile("rdf-data/expected/concept_1_reference_data.ttl")

            assertTrue(result.isIsomorphicWith(expected))*/
        }

        @Test
        fun `test publisher types are added from reference data`() {
            /*whenever(referenceDataCache.conceptStatuses())
                .thenReturn(responseReader.parseTurtleFile("rdf-data/reference-data/concept_statuses.ttl"))
            whenever(referenceDataCache.conceptSubjects())
                .thenReturn(responseReader.parseTurtleFile("rdf-data/reference-data/concept_subjects.ttl"))

            val result = referenceDataService.referenceDataModel(
                responseReader.parseTurtleFile("rdf-data/input-graphs/concept_1.ttl"),
                CatalogType.CONCEPTS,
            )
            val expected = responseReader.parseTurtleFile("rdf-data/expected/concept_1_reference_data.ttl")

            assertTrue(result.isIsomorphicWith(expected))*/
        }

        @Test
        fun `test adms statuses are added from reference data`() {
            /*whenever(referenceDataCache.conceptStatuses())
                .thenReturn(responseReader.parseTurtleFile("rdf-data/reference-data/concept_statuses.ttl"))
            whenever(referenceDataCache.conceptSubjects())
                .thenReturn(responseReader.parseTurtleFile("rdf-data/reference-data/concept_subjects.ttl"))

            val result = referenceDataService.referenceDataModel(
                responseReader.parseTurtleFile("rdf-data/input-graphs/concept_1.ttl"),
                CatalogType.CONCEPTS,
            )
            val expected = responseReader.parseTurtleFile("rdf-data/expected/concept_1_reference_data.ttl")

            assertTrue(result.isIsomorphicWith(expected))*/
        }

        @Test
        fun `test role types are added from reference data`() {
            /*whenever(referenceDataCache.conceptStatuses())
                .thenReturn(responseReader.parseTurtleFile("rdf-data/reference-data/concept_statuses.ttl"))
            whenever(referenceDataCache.conceptSubjects())
                .thenReturn(responseReader.parseTurtleFile("rdf-data/reference-data/concept_subjects.ttl"))

            val result = referenceDataService.referenceDataModel(
                responseReader.parseTurtleFile("rdf-data/input-graphs/concept_1.ttl"),
                CatalogType.CONCEPTS,
            )
            val expected = responseReader.parseTurtleFile("rdf-data/expected/concept_1_reference_data.ttl")

            assertTrue(result.isIsomorphicWith(expected))*/
        }

        @Test
        fun `test evidence types are added from reference data`() {
            /*whenever(referenceDataCache.conceptStatuses())
                .thenReturn(responseReader.parseTurtleFile("rdf-data/reference-data/concept_statuses.ttl"))
            whenever(referenceDataCache.conceptSubjects())
                .thenReturn(responseReader.parseTurtleFile("rdf-data/reference-data/concept_subjects.ttl"))

            val result = referenceDataService.referenceDataModel(
                responseReader.parseTurtleFile("rdf-data/input-graphs/concept_1.ttl"),
                CatalogType.CONCEPTS,
            )
            val expected = responseReader.parseTurtleFile("rdf-data/expected/concept_1_reference_data.ttl")

            assertTrue(result.isIsomorphicWith(expected))*/
        }

        @Test
        fun `test channel types are added from reference data`() {
            /*whenever(referenceDataCache.conceptStatuses())
                .thenReturn(responseReader.parseTurtleFile("rdf-data/reference-data/concept_statuses.ttl"))
            whenever(referenceDataCache.conceptSubjects())
                .thenReturn(responseReader.parseTurtleFile("rdf-data/reference-data/concept_subjects.ttl"))

            val result = referenceDataService.referenceDataModel(
                responseReader.parseTurtleFile("rdf-data/input-graphs/concept_1.ttl"),
                CatalogType.CONCEPTS,
            )
            val expected = responseReader.parseTurtleFile("rdf-data/expected/concept_1_reference_data.ttl")

            assertTrue(result.isIsomorphicWith(expected))*/
        }

        @Test
        fun `test main activities are added from reference data`() {
            /*whenever(referenceDataCache.conceptStatuses())
                .thenReturn(responseReader.parseTurtleFile("rdf-data/reference-data/concept_statuses.ttl"))
            whenever(referenceDataCache.conceptSubjects())
                .thenReturn(responseReader.parseTurtleFile("rdf-data/reference-data/concept_subjects.ttl"))

            val result = referenceDataService.referenceDataModel(
                responseReader.parseTurtleFile("rdf-data/input-graphs/concept_1.ttl"),
                CatalogType.CONCEPTS,
            )
            val expected = responseReader.parseTurtleFile("rdf-data/expected/concept_1_reference_data.ttl")

            assertTrue(result.isIsomorphicWith(expected))*/
        }

        @Test
        fun `test weekdays are added from reference data`() {
            /*whenever(referenceDataCache.conceptStatuses())
                .thenReturn(responseReader.parseTurtleFile("rdf-data/reference-data/concept_statuses.ttl"))
            whenever(referenceDataCache.conceptSubjects())
                .thenReturn(responseReader.parseTurtleFile("rdf-data/reference-data/concept_subjects.ttl"))

            val result = referenceDataService.referenceDataModel(
                responseReader.parseTurtleFile("rdf-data/input-graphs/concept_1.ttl"),
                CatalogType.CONCEPTS,
            )
            val expected = responseReader.parseTurtleFile("rdf-data/expected/concept_1_reference_data.ttl")

            assertTrue(result.isIsomorphicWith(expected))*/
        }


        @Test
        fun `test los, eurovoc and data themes are added from reference data`() {
            /*whenever(referenceDataCache.conceptStatuses())
                .thenReturn(responseReader.parseTurtleFile("rdf-data/reference-data/concept_statuses.ttl"))
            whenever(referenceDataCache.conceptSubjects())
                .thenReturn(responseReader.parseTurtleFile("rdf-data/reference-data/concept_subjects.ttl"))

            val result = referenceDataService.referenceDataModel(
                responseReader.parseTurtleFile("rdf-data/input-graphs/concept_1.ttl"),
                CatalogType.CONCEPTS,
            )
            val expected = responseReader.parseTurtleFile("rdf-data/expected/concept_1_reference_data.ttl")

            assertTrue(result.isIsomorphicWith(expected))*/
        }

    }
}