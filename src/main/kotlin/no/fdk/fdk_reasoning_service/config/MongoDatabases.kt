package no.fdk.fdk_reasoning_service.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties("application.databases")
data class MongoDatabases(
    val datasets: String,
    val events: String,
    val publicServices: String,
    val infoModels: String
)
