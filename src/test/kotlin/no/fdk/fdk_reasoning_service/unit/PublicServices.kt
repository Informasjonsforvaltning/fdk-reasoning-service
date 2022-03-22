package no.fdk.fdk_reasoning_service.unit

import com.nhaarman.mockitokotlin2.*
import no.fdk.fdk_reasoning_service.model.TurtleDBO
import no.fdk.fdk_reasoning_service.service.*
import no.fdk.fdk_reasoning_service.utils.TestResponseReader
import no.fdk.fdk_reasoning_service.utils.allPublicServiceIds
import org.apache.jena.riot.Lang
import org.junit.jupiter.api.*
import org.springframework.data.mongodb.core.MongoTemplate
import java.util.*
import kotlin.test.assertTrue

@Tag("unit")
class PublicServices {
    private val publicServiceMongoTemplate: MongoTemplate = mock()
    private val reasoningService: ReasoningService = mock()
    private val publicServiceService = PublicServiceService(reasoningService, publicServiceMongoTemplate)
    private val responseReader = TestResponseReader()

    @Test
    fun testPublicServices() {
        val servicesModel = responseReader.parseTurtleFile("fdk_ready_public_services.ttl")
        whenever(publicServiceMongoTemplate.findById<TurtleDBO>(any(), any(), any()))
            .thenReturn(TurtleDBO("unionId", gzip("")))
        whenever(reasoningService.catalogReasoning(any(), any()))
            .thenReturn(servicesModel)

        publicServiceService.reasonHarvestedPublicServices()

        argumentCaptor<TurtleDBO, String>().apply {
            verify(publicServiceMongoTemplate, times(19)).save(first.capture(), second.capture())
            Assertions.assertEquals(allPublicServiceIds, first.allValues.map { it.id })
            Assertions.assertEquals(Collections.nCopies(19, "fdkPublicService"), second.allValues)

            val savedUnion = parseRDFResponse(ungzip(first.firstValue.turtle), Lang.TURTLE, "")
            assertTrue(servicesModel.isIsomorphicWith(savedUnion))

            val expectedService = responseReader.parseTurtleFile("public_service_0.ttl")
            val savedService = parseRDFResponse(ungzip(first.allValues[10].turtle), Lang.TURTLE, "")
            assertTrue(expectedService.isIsomorphicWith(savedService))
        }
    }

    @Test
    fun testPublicServicesError() {
        whenever(publicServiceMongoTemplate.findById<TurtleDBO>(any(), any(), any()))
            .thenReturn(TurtleDBO("unionId", gzip("")))
        whenever(reasoningService.catalogReasoning(any(), any()))
            .thenThrow(RuntimeException())

        assertDoesNotThrow { publicServiceService.reasonHarvestedPublicServices() }

        argumentCaptor<TurtleDBO, String>().apply {
            verify(publicServiceMongoTemplate, times(0)).save(first.capture(), second.capture())
        }
    }

}
