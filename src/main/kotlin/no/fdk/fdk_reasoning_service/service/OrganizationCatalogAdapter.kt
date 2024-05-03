package no.fdk.fdk_reasoning_service.service

import org.apache.jena.rdf.model.Resource
import org.apache.jena.riot.Lang
import org.apache.jena.riot.RDFDataMgr
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder

@Service
class OrganizationCatalogAdapter {

    private val logger: Logger = LoggerFactory.getLogger(OrganizationService::class.java)

    fun orgPathAdapter(value: String, orgBaseURI: String): String? {
        val uri = "$orgBaseURI/orgpath/${URLEncoder.encode(value, "UTF-8")}"
        with(URI(uri).toURL().openConnection() as HttpURLConnection) {
            setRequestProperty("Accept", "text/plain")
            try {
                if (HttpStatus.valueOf(responseCode).is2xxSuccessful) {
                    return inputStream.bufferedReader().use(BufferedReader::readText)
                } else {
                    logger.error(
                        "Fetch of orgPath for value $value failed, status: $responseCode",
                        Exception("Fetch of orgPath for value $value failed")
                    )
                }
            } catch (ex: Exception) {
                logger.error("Error fetching orgPath for value $value", ex)
            } finally {
                disconnect()
            }
            return null
        }
    }

    fun downloadOrgData(uri: String): Resource? =
        try {
            RDFDataMgr.loadModel(uri, Lang.TURTLE).getResource(uri)
        } catch (ex: Exception) {
            logger.debug("Failed to fetch organization data for $uri")
            null
        }
}
