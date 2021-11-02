package no.fdk.fdk_reasoning_service.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties("spring.data.mongodb")
data class MongoConnectionString(
    val uri: String
)
