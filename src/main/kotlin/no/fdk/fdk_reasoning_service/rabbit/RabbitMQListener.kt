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
        logger.info("Received message with key ${message.messageProperties.receivedRoutingKey}")
        try {
            val reports = jacksonObjectMapper().readValue<List<HarvestReport>>(message.body)
            logger.info("Received message with body $reports")
        } catch (ex: Exception) {
            logger.error("sdf", ex)
        }

        reasoningActivity.initiateReasoning(message.messageProperties.receivedRoutingKey)
    }

}
