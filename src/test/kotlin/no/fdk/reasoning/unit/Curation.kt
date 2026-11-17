package no.fdk.reasoning.unit

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.fdk.reasoning.cache.CurationCache
import no.fdk.reasoning.cache.InvalidCurationFileException
import no.fdk.reasoning.cache.ReferenceDataCache
import no.fdk.reasoning.cache.parseDatasetTypes
import no.fdk.reasoning.config.ApplicationURI
import no.fdk.reasoning.model.CatalogType
import no.fdk.reasoning.service.CurationService
import no.fdk.reasoning.utils.ApiTestContext
import no.fdk.reasoning.utils.TestResponseReader
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.RDFNode
import org.apache.jena.vocabulary.DCAT
import org.apache.jena.vocabulary.DCTerms
import org.apache.jena.vocabulary.RDF
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

private const val FDK_ID = "4667277a-9d27-32c1-aed5-612fa601f393"
private const val DATASET_URI = "http://brreg.no/catalogs/910244132/datasets/c32b7a4f-655f-45f6-88f6-d01f05d0f7c2"
private const val NAMESPACE = "https://data.norge.no/vocabulary/high-value-dataset-type#"
private const val EU_DATASET_TYPE = "http://publications.europa.eu/resource/authority/dataset-type/STATISTICAL"
private val KNOWN_TYPES = setOf("${NAMESPACE}eu-hvd", "${NAMESPACE}national-hvd", "${NAMESPACE}national-basic-data")

@Tag("unit")
class Curation : ApiTestContext() {
    private val responseReader = TestResponseReader()

    @Nested
    internal inner class Parse {
        private val header = "fdkId,title,eu-hvd,national-hvd,national-basic-data"

        @Test
        fun `parses codes marked ja`() {
            val result =
                parseDatasetTypes(
                    """
                    $header
                    $FDK_ID,"Tittel, med komma",ja,nei,Ja
                    other-id,Annet,nei,nei,nei
                    """.trimIndent(),
                    KNOWN_TYPES,
                )

            assertEquals(setOf("${NAMESPACE}eu-hvd", "${NAMESPACE}national-basic-data"), result[FDK_ID])
            assertEquals(emptySet(), result["other-id"])
        }

        @Test
        fun `ignores byte order mark and empty lines`() {
            val result = parseDatasetTypes("\uFEFFfdkId,title,eu-hvd\n\n$FDK_ID,Tittel,ja\n", KNOWN_TYPES)

            assertEquals(setOf("${NAMESPACE}eu-hvd"), result[FDK_ID])
        }

        @Test
        fun `header only gives empty result`() {
            assertEquals(emptyMap(), parseDatasetTypes("$header\n", KNOWN_TYPES))
        }

        @Test
        fun `fails when required columns are missing`() {
            assertFailsWith<InvalidCurationFileException> {
                parseDatasetTypes("id,title,eu-hvd\n$FDK_ID,Tittel,ja\n", KNOWN_TYPES)
            }
            assertFailsWith<InvalidCurationFileException> {
                parseDatasetTypes("fdkId;title;eu-hvd\n", KNOWN_TYPES)
            }
        }

        @Test
        fun `fails when type columns are unknown or missing`() {
            assertFailsWith<InvalidCurationFileException> {
                parseDatasetTypes("fdkId,title,eu-hvd,unknown-type\n$FDK_ID,Tittel,ja,nei\n", KNOWN_TYPES)
            }
            assertFailsWith<InvalidCurationFileException> {
                parseDatasetTypes("fdkId,title\n$FDK_ID,Tittel\n", KNOWN_TYPES)
            }
        }

        @Test
        fun `skips invalid rows and keeps valid rows`() {
            val result =
                parseDatasetTypes(
                    """
                    $header
                    $FDK_ID,Tittel,ja,nei,nei
                    wrong-columns,Tittel,ja,nei
                    ,Mangler fdkId,ja,nei,nei
                    wrong-value,Tittel,x,nei,nei
                    empty-value,Tittel,ja,,nei
                    """.trimIndent(),
                    KNOWN_TYPES,
                )

            assertEquals(mapOf(FDK_ID to setOf("${NAMESPACE}eu-hvd")), result)
        }

        @Test
        fun `uses last line when fdkId appears more than once`() {
            val result =
                parseDatasetTypes(
                    """
                    $header
                    $FDK_ID,Tittel,ja,nei,nei
                    $FDK_ID,Tittel,nei,ja,nei
                    """.trimIndent(),
                    KNOWN_TYPES,
                )

            assertEquals(setOf("${NAMESPACE}national-hvd"), result[FDK_ID])
        }
    }

    @Nested
    internal inner class Cache {
        private val uris: ApplicationURI = mockk()
        private val referenceDataCache: ReferenceDataCache = mockk()
        private val cache = CurationCache(uris, referenceDataCache)

        @Test
        fun `able to update and keeps old data when update fails`() {
            every { referenceDataCache.highValueDatasetTypes() } returns
                responseReader.parseTurtleFile("rdf-data/reference-data/high_value_dataset_types.ttl")
            every { uris.curatedDatasetTypes } returns
                "http://localhost:5050/curation/high-value-dataset-types.csv" andThen
                "http://localhost:5050/404"

            assertTrue(!cache.isLoaded())
            cache.updateDatasetTypes()
            assertEquals(setOf("${NAMESPACE}eu-hvd", "${NAMESPACE}national-basic-data"), cache.datasetTypes(FDK_ID))
            cache.updateDatasetTypes()
            assertTrue(cache.isLoaded())
            assertEquals(setOf("${NAMESPACE}eu-hvd", "${NAMESPACE}national-basic-data"), cache.datasetTypes(FDK_ID))
        }

        @Test
        fun `updates high value dataset types when missing`() {
            every { referenceDataCache.highValueDatasetTypes() } returns
                ModelFactory.createDefaultModel() andThen
                responseReader.parseTurtleFile("rdf-data/reference-data/high_value_dataset_types.ttl")
            every { referenceDataCache.updateHighValueDatasetTypes() } just runs
            every { uris.curatedDatasetTypes } returns "http://localhost:5050/curation/high-value-dataset-types.csv"

            cache.updateDatasetTypes()

            verify { referenceDataCache.updateHighValueDatasetTypes() }
            assertTrue(cache.isLoaded())
        }

        @Test
        fun `is not loaded when file has invalid structure`() {
            every { referenceDataCache.highValueDatasetTypes() } returns ModelFactory.createDefaultModel()
            every { referenceDataCache.updateHighValueDatasetTypes() } just runs
            every { uris.curatedDatasetTypes } returns "http://localhost:5050/curation/high-value-dataset-types.csv"

            cache.updateDatasetTypes()

            assertTrue(!cache.isLoaded())
        }
    }

    @Nested
    internal inner class Service {
        private val curationCache: CurationCache = mockk()
        private val curationService = CurationService(curationCache)

        private fun Model.datasetTypes(): Set<String> = listObjectsOfProperty(getResource(DATASET_URI), DCTerms.type)
            .toList()
            .map { it.asResource().uri }
            .toSet()

        private fun inputWithPublisherTypes(): Model {
            val input = responseReader.parseTurtleFile("rdf-data/input-graphs/dataset.ttl")
            val dataset = input.getResource(DATASET_URI)
            dataset.addProperty(DCTerms.type, input.createResource("${NAMESPACE}national-hvd"))
            dataset.addProperty(DCTerms.type, input.createResource(EU_DATASET_TYPE))
            return input
        }

        @Test
        fun `replaces publisher codes with curated codes`() {
            every { curationCache.isLoaded() } returns true
            every { curationCache.datasetTypes(FDK_ID) } returns setOf("${NAMESPACE}eu-hvd")

            val input = inputWithPublisherTypes()
            curationService.curate(input, null, CatalogType.DATASETS)

            assertEquals(setOf("${NAMESPACE}eu-hvd", EU_DATASET_TYPE), input.datasetTypes())
        }

        @Test
        fun `finds catalog record in catalog graph`() {
            every { curationCache.isLoaded() } returns true
            every { curationCache.datasetTypes(FDK_ID) } returns setOf("${NAMESPACE}eu-hvd")

            val input = inputWithPublisherTypes()
            val catalogModel = ModelFactory.createDefaultModel()
            input
                .listResourcesWithProperty(RDF.type, DCAT.CatalogRecord)
                .toList()
                .forEach { record -> catalogModel.add(record.listProperties()) }
            catalogModel.listStatements().toList().forEach { input.remove(it) }

            curationService.curate(input, catalogModel, CatalogType.DATASETS)

            assertEquals(setOf("${NAMESPACE}eu-hvd", EU_DATASET_TYPE), input.datasetTypes())
            assertTrue(catalogModel.listStatements(null, DCTerms.type, null as RDFNode?).toList().isEmpty())
        }

        @Test
        fun `removes publisher codes from datasets without curated codes`() {
            every { curationCache.isLoaded() } returns true
            every { curationCache.datasetTypes(FDK_ID) } returns emptySet()

            val input = inputWithPublisherTypes()
            curationService.curate(input, null, CatalogType.DATASETS)

            assertEquals(setOf(EU_DATASET_TYPE), input.datasetTypes())
        }

        @Test
        fun `does not change other catalog types`() {
            val input = inputWithPublisherTypes()
            val expected = inputWithPublisherTypes()
            curationService.curate(input, null, CatalogType.DATASERVICES)

            assertTrue(input.isIsomorphicWith(expected))
        }

        @Test
        fun `exception is thrown when curated dataset types are not loaded`() {
            every { curationCache.isLoaded() } returns false

            assertFailsWith<Exception> {
                curationService.curate(inputWithPublisherTypes(), null, CatalogType.DATASETS)
            }
        }
    }
}
