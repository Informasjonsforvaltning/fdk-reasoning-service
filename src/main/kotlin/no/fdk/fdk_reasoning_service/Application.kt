package no.fdk.fdk_reasoning_service

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration
import org.springframework.boot.context.properties.ConfigurationPropertiesScan

@SpringBootApplication(exclude = [MongoAutoConfiguration::class, MongoDataAutoConfiguration::class])
@ConfigurationPropertiesScan
open class Application

fun main(args: Array<String>) {
    SpringApplication.run(Application::class.java, *args)
}
