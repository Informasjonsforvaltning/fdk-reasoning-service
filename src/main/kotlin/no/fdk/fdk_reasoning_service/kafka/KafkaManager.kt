package no.fdk.fdk_reasoning_service.kafka

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import org.springframework.kafka.config.KafkaListenerEndpointRegistry
import org.springframework.stereotype.Component

@Component
class KafkaManager (
    private val registry: KafkaListenerEndpointRegistry
) {
    fun pause(id: String) {
        Logger.debug("Pausing kafka listener containers with id: $id")
        registry.listenerContainers
            .filter { it.listenerId.equals(id) }
            .forEach { it.pause() }
    }

    fun resume(id: String) {
        Logger.debug("Resuming kafka listener containers with id: $id")
        registry.listenerContainers
            .filter { it.listenerId.equals(id) }
            .forEach { it.resume() }
    }

    companion object {
        private val Logger: Logger = LoggerFactory.getLogger(KafkaManager::class.java)
    }
}
