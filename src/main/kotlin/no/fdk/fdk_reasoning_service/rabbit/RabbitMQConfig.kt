package no.fdk.fdk_reasoning_service.rabbit

import org.springframework.amqp.core.*
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class RabbitMQConfig {

    @Bean
    open fun receiverQueue(): Queue = AnonymousQueue()

    @Bean
    open fun topicExchange(): TopicExchange =
        ExchangeBuilder
            .topicExchange("harvests")
            .durable(false)
            .build()

    @Bean
    open fun binding(topicExchange: TopicExchange?, receiverQueue: Queue?): Binding =
        BindingBuilder
            .bind(receiverQueue)
            .to(topicExchange)
            .with("*.harvested")
}
