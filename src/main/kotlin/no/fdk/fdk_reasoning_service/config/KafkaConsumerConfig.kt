package no.fdk.fdk_reasoning_service.config

import no.fdk.concept.ConceptEvent
import no.fdk.dataservice.DataServiceEvent
import no.fdk.dataset.DatasetEvent
import no.fdk.event.EventEvent
import no.fdk.informationmodel.InformationModelEvent
import no.fdk.service.ServiceEvent
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
    open fun kafkaDatasetListenerContainerFactory(consumerFactory: ConsumerFactory<String, DatasetEvent>): ConcurrentKafkaListenerContainerFactory<String, DatasetEvent> {
        val factory: ConcurrentKafkaListenerContainerFactory<String, DatasetEvent> =
            ConcurrentKafkaListenerContainerFactory()
        factory.consumerFactory = consumerFactory
        factory.containerProperties.ackMode = ContainerProperties.AckMode.MANUAL_IMMEDIATE
        return factory
    }

    @Bean
    open fun kafkaConceptListenerContainerFactory(consumerFactory: ConsumerFactory<String, ConceptEvent>): ConcurrentKafkaListenerContainerFactory<String, ConceptEvent> {
        val factory: ConcurrentKafkaListenerContainerFactory<String, ConceptEvent> =
            ConcurrentKafkaListenerContainerFactory()
        factory.consumerFactory = consumerFactory
        factory.containerProperties.ackMode = ContainerProperties.AckMode.MANUAL_IMMEDIATE
        return factory
    }

    @Bean
    open fun kafkaDataServiceListenerContainerFactory(consumerFactory: ConsumerFactory<String, DataServiceEvent>): ConcurrentKafkaListenerContainerFactory<String, DataServiceEvent> {
        val factory: ConcurrentKafkaListenerContainerFactory<String, DataServiceEvent> =
            ConcurrentKafkaListenerContainerFactory()
        factory.consumerFactory = consumerFactory
        factory.containerProperties.ackMode = ContainerProperties.AckMode.MANUAL_IMMEDIATE
        return factory
    }

    @Bean
    open fun kafkaInformationModelListenerContainerFactory(consumerFactory: ConsumerFactory<String, InformationModelEvent>): ConcurrentKafkaListenerContainerFactory<String, InformationModelEvent> {
        val factory: ConcurrentKafkaListenerContainerFactory<String, InformationModelEvent> =
            ConcurrentKafkaListenerContainerFactory()
        factory.consumerFactory = consumerFactory
        factory.containerProperties.ackMode = ContainerProperties.AckMode.MANUAL_IMMEDIATE
        return factory
    }

    @Bean
    open fun kafkaServiceListenerContainerFactory(consumerFactory: ConsumerFactory<String, ServiceEvent>): ConcurrentKafkaListenerContainerFactory<String, ServiceEvent> {
        val factory: ConcurrentKafkaListenerContainerFactory<String, ServiceEvent> =
            ConcurrentKafkaListenerContainerFactory()
        factory.consumerFactory = consumerFactory
        factory.containerProperties.ackMode = ContainerProperties.AckMode.MANUAL_IMMEDIATE
        return factory
    }

    @Bean
    open fun kafkaEventListenerContainerFactory(consumerFactory: ConsumerFactory<String, EventEvent>): ConcurrentKafkaListenerContainerFactory<String, EventEvent> {
        val factory: ConcurrentKafkaListenerContainerFactory<String, EventEvent> =
            ConcurrentKafkaListenerContainerFactory()
        factory.consumerFactory = consumerFactory
        factory.containerProperties.ackMode = ContainerProperties.AckMode.MANUAL_IMMEDIATE
        return factory
    }
}
