package no.fdk.fdk_reasoning_service.service

import no.fdk.fdk_reasoning_service.config.ApplicationURI
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

private val LOGGER: Logger = LoggerFactory.getLogger(ReasoningService::class.java)

@Service
class ReasoningService(
    private val uris: ApplicationURI
) {
    fun transformCatalogForSPARQL() {
        LOGGER.debug("Starting reasoning")
    }

}
