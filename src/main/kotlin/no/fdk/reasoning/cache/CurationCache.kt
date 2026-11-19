package no.fdk.reasoning.cache

import no.fdk.reasoning.config.ApplicationURI
import no.fdk.reasoning.metrics.ReferenceDataMetrics
import org.apache.commons.csv.CSVFormat
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.io.StringReader
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import kotlin.time.measureTimedValue

private val logger: Logger = LoggerFactory.getLogger(CurationCache::class.java)

const val HIGH_VALUE_DATASET_TYPE_NAMESPACE = "https://data.norge.no/vocabulary/high-value-dataset-type#"

private const val VERSION_HEADER = "X-Curation-Version"
private val REQUIRED_COLUMNS = listOf("fdkId", "title")
private val VALUES = setOf("ja", "nei")

class InvalidCurationFileException(message: String) : Exception(message)

@Service
class CurationCache(private val uris: ApplicationURI, private val referenceDataCache: ReferenceDataCache) {
    private val httpClient: HttpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()

    @Volatile
    private var datasetTypes: Map<String, Set<String>>? = null

    @Volatile
    private var etag: String? = null

    @Volatile
    private var version: String? = null

    fun isLoaded(): Boolean = datasetTypes != null

    fun version(): String? = version

    fun datasetTypes(fdkId: String): Set<String> = datasetTypes?.get(fdkId) ?: emptySet()

    @EventListener
    @Order(Ordered.LOWEST_PRECEDENCE)
    fun loadCacheOnStartup(event: ApplicationReadyEvent) {
        updateDatasetTypes()
    }

    @Scheduled(
        fixedDelayString = "\${application.curation.refreshInterval:PT5M}",
        initialDelayString = "\${application.curation.refreshInterval:PT5M}",
    )
    fun updateDatasetTypes() {
        val timed =
            measureTimedValue {
                try {
                    download()?.let { response ->
                        val knownTypes =
                            referenceDataCache
                                .highValueDatasetTypes()
                                .listSubjects()
                                .toList()
                                .mapNotNull { it.uri }
                                .toSet()
                        val parsed = parseDatasetTypes(response.body(), knownTypes)
                        datasetTypes = parsed
                        etag = response.headers().firstValue("ETag").orElse(null)
                        version = response.headers().firstValue(VERSION_HEADER).orElse(null)
                        logger.info("Loaded curated dataset types version $version with ${parsed.size} datasets")
                        true
                    }
                } catch (ex: Exception) {
                    logger.error("Update of curated dataset types from ${uris.curatedDatasetTypes} failed", ex)
                    false
                }
            }
        timed.value?.let { ReferenceDataMetrics.recordRefresh("curated dataset types", it, timed.duration) }
    }

    private fun download(): HttpResponse<String>? {
        val request =
            HttpRequest
                .newBuilder(URI(uris.curatedDatasetTypes))
                .timeout(Duration.ofSeconds(30))
                .GET()
                .apply { if (datasetTypes != null) etag?.let { header("If-None-Match", it) } }
                .build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        return when (response.statusCode()) {
            200 -> response
            304 -> null
            else -> throw IllegalStateException("Unexpected status ${response.statusCode()}")
        }
    }
}

fun parseDatasetTypes(csv: String, knownTypes: Set<String>): Map<String, Set<String>> {
    val parser =
        CSVFormat.DEFAULT
            .builder()
            .setHeader()
            .setSkipHeaderRecord(true)
            .setIgnoreSurroundingSpaces(true)
            .setIgnoreEmptyLines(true)
            .get()
            .parse(StringReader(csv.removePrefix("\uFEFF")))

    val header = parser.headerNames
    if (header.take(REQUIRED_COLUMNS.size) != REQUIRED_COLUMNS) {
        throw InvalidCurationFileException(
            "Header must start with ${REQUIRED_COLUMNS.joinToString(",")}, found ${header.joinToString(",")}",
        )
    }
    val codes = header.drop(REQUIRED_COLUMNS.size)
    val unknownCodes = codes.filter { HIGH_VALUE_DATASET_TYPE_NAMESPACE + it !in knownTypes }
    if (codes.isEmpty() || unknownCodes.isNotEmpty()) {
        throw InvalidCurationFileException("Unknown or missing high-value dataset type columns: ${unknownCodes.joinToString(",")}")
    }

    val result = mutableMapOf<String, Set<String>>()
    parser.forEach { record ->
        val line = record.recordNumber + 1
        val fdkId = record.get(0)
        val values = record.toList().drop(REQUIRED_COLUMNS.size).map { it.lowercase() }
        when {
            !record.isConsistent -> {
                logger.error("Skipping curated dataset types line $line: expected ${header.size} columns, found ${record.size()}")
            }

            fdkId.isBlank() -> {
                logger.error("Skipping curated dataset types line $line: missing fdkId")
            }

            values.any { it !in VALUES } -> {
                logger.error("Skipping curated dataset types line $line: values must be ja or nei")
            }

            else -> {
                if (fdkId in result) logger.warn("fdkId $fdkId appears more than once in curated dataset types, using line $line")
                result[fdkId] =
                    codes
                        .zip(values)
                        .filter { (_, value) -> value == "ja" }
                        .map { (code, _) -> HIGH_VALUE_DATASET_TYPE_NAMESPACE + code }
                        .toSet()
            }
        }
    }
    return result
}
