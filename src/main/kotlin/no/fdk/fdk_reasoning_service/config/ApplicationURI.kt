package no.fdk.fdk_reasoning_service.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties("application.uri")
data class ApplicationURI(
    val organizations: String,
    val dataservices: String,
    val concepts: String,
    val informationmodels: String,
    val datasets: String
)
