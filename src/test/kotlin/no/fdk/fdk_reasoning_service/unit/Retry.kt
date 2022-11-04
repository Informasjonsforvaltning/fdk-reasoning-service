package no.fdk.fdk_reasoning_service.unit

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
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
    private val uris: ApplicationURI = mock()
    private val reasoningActivity = ReasoningActivity(
        conceptService,
        dataServiceService,
        datasetService,
        eventService,
        infoModelService,
        publicServiceService,
        rabbitMQPublisher,
        uris
    )

    @BeforeEach
    fun beforeEach() {
        RETRY_QUEUE.removeAll { retry -> retry != null }
    }

    @Test
    fun testRetryQueue() {
        whenever(uris.los)
            .thenReturn("http://localhost:$LOCAL_SERVER_PORT/wrong-los-url")
        whenever(uris.orgExternal)
            .thenReturn("http://localhost:$LOCAL_SERVER_PORT/wrong-org-url")
        whenever(uris.orgInternal)
            .thenReturn("http://localhost:$LOCAL_SERVER_PORT/wrong-org-url")

        reasoningActivity.initiateReasoning(CatalogType.DATASETS, emptyList(), 0)
        assertTrue { RETRY_QUEUE.contains(RetryReportsWrap(CatalogType.DATASETS, 1, emptyList())) }
    }

    @Test
    fun testTenthRetryNotAddedToQueue() {
        whenever(uris.los)
            .thenReturn("http://localhost:$LOCAL_SERVER_PORT/wrong-los-url")
        whenever(uris.orgInternal)
            .thenReturn("http://localhost:$LOCAL_SERVER_PORT/wrong-org-url")
        whenever(uris.orgExternal)
            .thenReturn("http://localhost:$LOCAL_SERVER_PORT/wrong-org-url")

        reasoningActivity.initiateReasoning(CatalogType.DATASETS, emptyList(), 10)
        assertTrue { RETRY_QUEUE.isEmpty() }
    }

}
