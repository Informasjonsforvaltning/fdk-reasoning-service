package no.fdk.fdk_reasoning_service.rabbit

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

        reasoningActivity.initiateReasoning(message.messageProperties.receivedRoutingKey)
    }

}
