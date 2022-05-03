package no.fdk.fdk_reasoning_service.rabbit

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.fdk.fdk_reasoning_service.model.HarvestReport
import no.fdk.fdk_reasoning_service.service.ReasoningActivity
import org.slf4j.LoggerFactory
import org.springframework.amqp.core.Message
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Service

private val logger = LoggerFactory.getLogger(RabbitMQListener::class.java)

@Service
class RabbitMQListener(
    private val reasoningActivity: ReasoningActivity
) {

    @RabbitListener(queues = ["#{receiverQueue.name}"])
    fun receiveMessage(message: Message) {
        val reports: List<HarvestReport> = try {
            jacksonObjectMapper().readValue(message.body)
        } catch (ex: Exception) {
            logger.error("Unable to parse harvest reports", ex)
            emptyList()
        }
        reasoningActivity.initiateReasoning(message.messageProperties.receivedRoutingKey, reports)
    }

}
