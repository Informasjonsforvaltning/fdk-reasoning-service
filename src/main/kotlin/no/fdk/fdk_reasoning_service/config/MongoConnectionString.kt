package no.fdk.fdk_reasoning_service.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("spring.data.mongodb")
data class MongoConnectionString(
    val uri: String
)
