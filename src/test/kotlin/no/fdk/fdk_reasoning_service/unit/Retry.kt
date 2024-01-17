package no.fdk.fdk_reasoning_service.unit

import no.fdk.fdk_reasoning_service.cache.ReferenceDataCache
import no.fdk.fdk_reasoning_service.config.ApplicationURI
import no.fdk.fdk_reasoning_service.model.CatalogType
import no.fdk.fdk_reasoning_service.model.RetryReportsWrap
import no.fdk.fdk_reasoning_service.rabbit.RabbitMQPublisher
import no.fdk.fdk_reasoning_service.service.ConceptService
import no.fdk.fdk_reasoning_service.service.DataServiceService
import no.fdk.fdk_reasoning_service.service.DatasetService
import no.fdk.fdk_reasoning_service.service.EventService
import no.fdk.fdk_reasoning_service.service.InfoModelService
import no.fdk.fdk_reasoning_service.service.PublicServiceService
import no.fdk.fdk_reasoning_service.service.RETRY_QUEUE
import no.fdk.fdk_reasoning_service.service.ReasoningActivity
import no.fdk.fdk_reasoning_service.utils.LOCAL_SERVER_PORT
import no.fdk.fdk_reasoning_service.utils.RDF_DATA
import org.apache.jena.rdf.model.ModelFactory
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertTrue

@Tag("unit")
class Retry {
    private val conceptService: ConceptService = mock()
    private val dataServiceService: DataServiceService = mock()
    private val datasetService: DatasetService = mock()
    private val eventService: EventService = mock()
    private val infoModelService: InfoModelService = mock()
    private val publicServiceService: PublicServiceService = mock()
    private val rabbitMQPublisher: RabbitMQPublisher = mock()
    private val referenceDataCache: ReferenceDataCache = mock()
    private val reasoningActivity = ReasoningActivity(
        conceptService,
        dataServiceService,
        datasetService,
        eventService,
        infoModelService,
        publicServiceService,
        rabbitMQPublisher,
        referenceDataCache
    )

    @BeforeEach
    fun beforeEach() {
        RETRY_QUEUE.removeAll { retry -> retry != null }
    }

    @Test
    fun testRetryQueue() {
        whenever(referenceDataCache.organizations())
            .thenReturn(ModelFactory.createDefaultModel())
        whenever(referenceDataCache.los())
            .thenReturn(ModelFactory.createDefaultModel())
        whenever(referenceDataCache.eurovocs())
            .thenReturn(ModelFactory.createDefaultModel())
        whenever(referenceDataCache.dataThemes())
            .thenReturn(ModelFactory.createDefaultModel())
        whenever(referenceDataCache.conceptStatuses())
            .thenReturn(ModelFactory.createDefaultModel())
        whenever(referenceDataCache.conceptSubjects())
            .thenReturn(ModelFactory.createDefaultModel())
        whenever(referenceDataCache.ianaMediaTypes())
            .thenReturn(ModelFactory.createDefaultModel())
        whenever(referenceDataCache.fileTypes())
            .thenReturn(ModelFactory.createDefaultModel())
        whenever(referenceDataCache.openLicenses())
            .thenReturn(ModelFactory.createDefaultModel())
        whenever(referenceDataCache.linguisticSystems())
            .thenReturn(ModelFactory.createDefaultModel())
        whenever(referenceDataCache.locations())
            .thenReturn(ModelFactory.createDefaultModel())
        whenever(referenceDataCache.accessRights())
            .thenReturn(ModelFactory.createDefaultModel())
        whenever(referenceDataCache.frequencies())
            .thenReturn(ModelFactory.createDefaultModel())
        whenever(referenceDataCache.provenance())
            .thenReturn(ModelFactory.createDefaultModel())
        whenever(referenceDataCache.publisherTypes())
            .thenReturn(ModelFactory.createDefaultModel())
        whenever(referenceDataCache.admsStatuses())
            .thenReturn(ModelFactory.createDefaultModel())

        reasoningActivity.initiateReasoning(CatalogType.DATASETS, emptyList(), 0)
        assertTrue { RETRY_QUEUE.contains(RetryReportsWrap(CatalogType.DATASETS, 1, emptyList())) }
    }

    @Test
    fun testTenthRetryNotAddedToQueue() {
        whenever(referenceDataCache.organizations())
            .thenReturn(RDF_DATA.orgData)
        whenever(referenceDataCache.los())
            .thenReturn(RDF_DATA.losData)
        whenever(referenceDataCache.eurovocs())
            .thenReturn(RDF_DATA.eurovocs)
        whenever(referenceDataCache.dataThemes())
            .thenReturn(RDF_DATA.dataThemes)
        whenever(referenceDataCache.conceptStatuses())
            .thenReturn(RDF_DATA.conceptStatuses)
        whenever(referenceDataCache.conceptSubjects())
            .thenReturn(RDF_DATA.conceptSubjects)
        whenever(referenceDataCache.ianaMediaTypes())
            .thenReturn(RDF_DATA.ianaMediaTypes)
        whenever(referenceDataCache.fileTypes())
            .thenReturn(RDF_DATA.fileTypes)
        whenever(referenceDataCache.openLicenses())
            .thenReturn(RDF_DATA.openLicenses)
        whenever(referenceDataCache.linguisticSystems())
            .thenReturn(RDF_DATA.linguisticSystems)
        whenever(referenceDataCache.locations())
            .thenReturn(RDF_DATA.locations)
        whenever(referenceDataCache.accessRights())
            .thenReturn(RDF_DATA.accessRights)
        whenever(referenceDataCache.frequencies())
            .thenReturn(RDF_DATA.frequencies)
        whenever(referenceDataCache.provenance())
            .thenReturn(RDF_DATA.provenance)
        whenever(referenceDataCache.publisherTypes())
            .thenReturn(RDF_DATA.publisherTypes)
        whenever(referenceDataCache.admsStatuses())
            .thenReturn(RDF_DATA.admsStatuses)

        reasoningActivity.initiateReasoning(CatalogType.DATASETS, emptyList(), 10)
        assertTrue { RETRY_QUEUE.isEmpty() }
    }

}
