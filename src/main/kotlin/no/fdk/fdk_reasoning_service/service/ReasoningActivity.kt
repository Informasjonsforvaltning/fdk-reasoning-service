package no.fdk.fdk_reasoning_service.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import no.fdk.fdk_reasoning_service.model.CatalogType
import no.fdk.fdk_reasoning_service.rabbit.RabbitMQPublisher
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.util.*

private val reasoningQueue: Queue<CatalogType> = LinkedList()
private val lastCatalogLaunches: MutableMap<CatalogType, LocalDateTime> = mutableMapOf()

@Component
class ReasoningActivity(
    private val conceptService: ConceptService,
    private val dataServiceService: DataServiceService,
    private val datasetService: DatasetService,
    private val eventService: EventService,
    private val infoModelService: InfoModelService,
    private val publicServiceService: PublicServiceService,
    private val rabbitMQPublisher: RabbitMQPublisher
) : CoroutineScope by CoroutineScope(Dispatchers.Default) {

    @Scheduled(fixedRate = 10000)
    private fun pollQueue() =
        reasoningQueue.poll()
            ?.launchIfNotLaunchedRecently()

    fun initiateReasoning(key: String) =
        catalogTypeFromRabbitMessageKey(key)
            ?.queueTransform()

    private fun CatalogType.queueTransform() {
        if (!reasoningQueue.contains(this)) reasoningQueue.add(this)
    }

    private fun CatalogType.launchIfNotLaunchedRecently() {
        val lastTransform = lastCatalogLaunches[this]
        when {
            lastTransform == null -> launchReasoning(this)
            lastTransform.isBefore(LocalDateTime.now().minusMinutes(15)) -> launchReasoning(this)
            else -> queueTransform()
        }
    }

    private fun launchReasoning(type: CatalogType) {
        lastCatalogLaunches[type] = LocalDateTime.now()
        val catalogReasoning = launch {
            when (type) {
                CatalogType.CONCEPTS -> conceptService.reasonHarvestedConcepts()
                CatalogType.DATASERVICES -> dataServiceService.reasonHarvestedDataServices()
                CatalogType.DATASETS -> datasetService.reasonHarvestedDatasets()
                CatalogType.EVENTS -> eventService.reasonHarvestedEvents()
                CatalogType.INFORMATIONMODELS -> infoModelService.reasonHarvestedInformationModels()
                CatalogType.PUBLICSERVICES -> publicServiceService.reasonHarvestedPublicServices()
            }
        }

        catalogReasoning.invokeOnCompletion { rabbitMQPublisher.send(type) }
    }

}
