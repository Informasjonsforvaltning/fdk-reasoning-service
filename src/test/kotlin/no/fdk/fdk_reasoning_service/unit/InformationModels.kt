package no.fdk.fdk_reasoning_service.unit

import com.nhaarman.mockitokotlin2.*
import no.fdk.fdk_reasoning_service.repository.InformationModelRepository
import no.fdk.fdk_reasoning_service.service.*
import no.fdk.fdk_reasoning_service.utils.INFOMODEL_0_ID
import no.fdk.fdk_reasoning_service.utils.TestResponseReader
import no.fdk.fdk_reasoning_service.utils.INFOMODEL_CATALOG_ID
import org.apache.jena.riot.Lang
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Tag("unit")
class InformationModels {
    private val repository: InformationModelRepository = mock()
    private val reasoningService: ReasoningService = mock()
    private val infoModelService = InfoModelService(reasoningService, repository)
    private val responseReader = TestResponseReader()

    @Test
    fun testInformationModels() {
        val infoModelsUnion = responseReader.parseTurtleFile("fdk_ready_infomodels.ttl")
        whenever(repository.findHarvestedUnion()).thenReturn("")
        whenever(reasoningService.catalogReasoning(any(), any()))
            .thenReturn(infoModelsUnion)

        infoModelService.reasonHarvestedInformationModels()

        argumentCaptor<String>().apply {
            verify(repository, times(1)).saveReasonedUnion(capture())
            val savedUnion = parseRDFResponse(firstValue, Lang.TURTLE, "")
            assertTrue(infoModelsUnion.isIsomorphicWith(savedUnion))
        }

        argumentCaptor<String, String>().apply {
            verify(repository, times(1)).saveCatalog(first.capture(), second.capture())
            val savedCatalog = parseRDFResponse(first.firstValue, Lang.TURTLE, "")
            assertTrue(infoModelsUnion.isIsomorphicWith(savedCatalog))
            assertEquals(INFOMODEL_CATALOG_ID, second.firstValue)
        }

        argumentCaptor<String, String>().apply {
            verify(repository, times(2)).saveInformationModel(first.capture(), second.capture())
            val expectedInfoModel = responseReader.parseTurtleFile("infomodel_0.ttl")
            val savedInfoModel = parseRDFResponse(first.firstValue, Lang.TURTLE, "")
            assertTrue(expectedInfoModel.isIsomorphicWith(savedInfoModel))
            assertEquals(INFOMODEL_0_ID, second.firstValue)
        }
    }

}
