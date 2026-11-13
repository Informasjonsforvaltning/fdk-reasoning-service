package no.fdk.reasoning.unit

import io.mockk.every
import io.mockk.mockk
import no.fdk.reasoning.cache.CurationCache
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

@Tag("unit")
class Curation : ApiTestContext() {
    private val responseReader = TestResponseReader()

    @Nested
    internal inner class Parse {
        @Test
        fun `parses codes marked ja`() {
            val result =
                parseDatasetTypes(
                    """
                    fdkId,title,eu-hvd,national-hvd,national-basic-data
                    $FDK_ID,"Tittel, med komma",ja,nei,Ja
                    other-id,Annet,nei,nei,
                    """.trimIndent(),
                )

            assertEquals(setOf("${NAMESPACE}eu-hvd", "${NAMESPACE}national-basic-data"), result[FDK_ID])
            assertEquals(emptySet(), result["other-id"])
        }

        @Test
        fun `ignores byte order mark and empty lines`() {
            val result = parseDatasetTypes("﻿fdkId,title,eu-hvd\n\n$FDK_ID,Tittel,ja\n")

            assertEquals(setOf("${NAMESPACE}eu-hvd"), result[FDK_ID])
        }
    }

    @Nested
    internal inner class Cache {
        private val uris: ApplicationURI = mockk()
        private val cache = CurationCache(uris)

        @Test
        fun `able to update and keeps old data when update fails`() {
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
