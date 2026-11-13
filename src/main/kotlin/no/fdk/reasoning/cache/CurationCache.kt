package no.fdk.reasoning.cache

import no.fdk.reasoning.config.ApplicationURI
import org.apache.commons.csv.CSVFormat
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.io.StringReader
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

private val logger: Logger = LoggerFactory.getLogger(CurationCache::class.java)

const val HIGH_VALUE_DATASET_TYPE_NAMESPACE = "https://data.norge.no/vocabulary/high-value-dataset-type#"

@Service
class CurationCache(private val uris: ApplicationURI) {
    private val httpClient: HttpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()

    @Volatile
    private var datasetTypes: Map<String, Set<String>>? = null

    fun isLoaded(): Boolean = datasetTypes != null

    fun datasetTypes(fdkId: String): Set<String> = datasetTypes?.get(fdkId) ?: emptySet()

    @EventListener
    fun loadCacheOnStartup(event: ApplicationReadyEvent) {
        updateDatasetTypes()
    }

    @Scheduled(cron = "0 */15 * * * ?")
    fun updateDatasetTypes() {
        try {
            val request =
                HttpRequest
                    .newBuilder(URI(uris.curatedDatasetTypes))
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .build()
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() != 200) {
                throw IllegalStateException("Unexpected status ${response.statusCode()}")
            }
            datasetTypes = parseDatasetTypes(response.body())
            logger.debug("successfully updated curated dataset types cache")
        } catch (ex: Exception) {
            logger.error("Download failed for ${uris.curatedDatasetTypes}", ex)
        }
    }
}

fun parseDatasetTypes(csv: String): Map<String, Set<String>> {
    val records =
        CSVFormat.DEFAULT
            .builder()
            .setHeader()
            .setSkipHeaderRecord(true)
            .setIgnoreSurroundingSpaces(true)
            .setIgnoreEmptyLines(true)
            .get()
            .parse(StringReader(csv.removePrefix("﻿")))

    val codes = records.headerNames.drop(2)

    return records
        .filter { it.get(0).isNotBlank() }
        .associate { record ->
            record.get(0) to
                codes
                    .filter { code -> record.get(code).equals("ja", ignoreCase = true) }
                    .map { code -> HIGH_VALUE_DATASET_TYPE_NAMESPACE + code }
                    .toSet()
        }
}
