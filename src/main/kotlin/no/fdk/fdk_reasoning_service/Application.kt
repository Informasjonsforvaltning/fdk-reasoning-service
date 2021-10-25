package no.fdk.fdk_reasoning_service

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
open class Application

fun main(args: Array<String>) {
    SpringApplication.run(Application::class.java, *args)
}
