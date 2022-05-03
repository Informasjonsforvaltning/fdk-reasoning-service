package no.fdk.fdk_reasoning_service.rabbit

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.fdk.fdk_reasoning_service.model.CatalogType
import no.fdk.fdk_reasoning_service.model.ReasoningReport
import org.slf4j.LoggerFactory
import org.springframework.amqp.AmqpException
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.stereotype.Service

private val LOGGER = LoggerFactory.getLogger(RabbitMQPublisher::class.java)

@Service
class RabbitMQPublisher(private val template: RabbitTemplate) {
    fun send(catalogType: CatalogType, reports: List<ReasoningReport>) {
        try {
            template.messageConverter = Jackson2JsonMessageConverter(jacksonObjectMapper())
            template.convertAndSend("harvests", catalogType.toReasonedKey(), reports)
            LOGGER.debug("Successfully sent reasoning completed message for $catalogType")
        } catch (e: AmqpException) {
            LOGGER.error("Could not send reasoned message", e)
        }
    }
}

private fun CatalogType.toReasonedKey(): String =
    when (this) {
        CatalogType.CONCEPTS -> "concepts.reasoned"
        CatalogType.DATASERVICES -> "dataservices.reasoned"
        CatalogType.DATASETS -> "datasets.reasoned"
        CatalogType.EVENTS -> "events.reasoned"
        CatalogType.INFORMATIONMODELS -> "informationmodels.reasoned"
        CatalogType.PUBLICSERVICES -> "public_services.reasoned"
    }
