package no.fdk.reasoning.config

import io.confluent.kafka.serializers.KafkaAvroSerializer
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory

@Configuration
open class KafkaProducerConfig(
    @param:Value("\${spring.kafka.bootstrap-servers}") private val bootstrapServers: String,
    @param:Value("\${spring.kafka.properties.security.protocol}") private val securityProtocol: String,
    @param:Value("\${spring.kafka.properties.sasl.mechanism}") private val saslMechanism: String,
    @param:Value("\${spring.kafka.properties.sasl.jaas.config}") private val saslJaasConfig: String,
    @param:Value("\${spring.kafka.properties.schema.registry.url}") private val schemaRegistryUrl: String,
) {
    @Bean
    open fun producerFactory(): ProducerFactory<String, SpecificRecord> {
        val props: MutableMap<String, Any> = HashMap()
        props[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServers
        props[CommonClientConfigs.SECURITY_PROTOCOL_CONFIG] = securityProtocol
        props[SaslConfigs.SASL_MECHANISM] = saslMechanism
        props[SaslConfigs.SASL_JAAS_CONFIG] = saslJaasConfig
        props[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        props[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = KafkaAvroSerializer::class.java
        props[ProducerConfig.COMPRESSION_TYPE_CONFIG] = "snappy"
        props["schema.registry.url"] = schemaRegistryUrl
        props["value.subject.name.strategy"] = "io.confluent.kafka.serializers.subject.RecordNameStrategy"
        return DefaultKafkaProducerFactory(props)
    }

    @Bean
    open fun kafkaTemplate(): KafkaTemplate<String, SpecificRecord> = KafkaTemplate(producerFactory())
}
