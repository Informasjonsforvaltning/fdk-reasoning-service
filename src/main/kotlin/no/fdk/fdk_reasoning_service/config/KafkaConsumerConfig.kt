package no.fdk.fdk_reasoning_service.config

import org.apache.avro.specific.SpecificRecord
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.listener.ContainerProperties

@EnableKafka
@Configuration
open class KafkaConsumerConfig {
    @Bean
    open fun kafkaListenerContainerFactory(
        consumerFactory: ConsumerFactory<String, SpecificRecord>,
    ): ConcurrentKafkaListenerContainerFactory<String, SpecificRecord> {
        val factory: ConcurrentKafkaListenerContainerFactory<String, SpecificRecord> =
            ConcurrentKafkaListenerContainerFactory()
        factory.consumerFactory = consumerFactory
        factory.containerProperties.ackMode = ContainerProperties.AckMode.MANUAL_IMMEDIATE
        return factory
    }
}
