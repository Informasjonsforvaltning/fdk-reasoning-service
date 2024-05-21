package no.fdk.fdk_reasoning_service.config

import org.apache.avro.specific.SpecificRecord
import org.springframework.context.annotation.Bean
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory

open class KafkaProducerConfig {
    @Bean
    open fun kafkaTemplate(producerFactory: ProducerFactory<String, SpecificRecord>): KafkaTemplate<String, SpecificRecord> {
        return KafkaTemplate(producerFactory)
    }
}
