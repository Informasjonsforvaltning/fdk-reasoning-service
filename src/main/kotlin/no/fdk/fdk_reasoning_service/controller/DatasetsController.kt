package no.fdk.fdk_reasoning_service.controller

import no.fdk.fdk_reasoning_service.service.DatasetService
import org.apache.jena.riot.Lang
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@CrossOrigin
@RequestMapping(
    value = ["/datasets"],
    produces = ["text/turtle", "text/n3", "application/rdf+json", "application/ld+json", "application/rdf+xml",
        "application/n-triples", "application/n-quads", "application/trig", "application/trix"]
)
class DatasetsController(private val datasetService: DatasetService) {

    @GetMapping("/{id}")
    fun getDatasetById(
        @RequestHeader(HttpHeaders.ACCEPT) accept: String?,
        @PathVariable id: String
    ): ResponseEntity<String> {
        val returnType = jenaLangFromAcceptHeader(accept)

        return if (returnType == Lang.RDFNULL) ResponseEntity(HttpStatus.NOT_ACCEPTABLE)
        else {
            datasetService.getDatasetById(id, returnType ?: Lang.TURTLE)
                ?.let { ResponseEntity(it, HttpStatus.OK) }
                ?: ResponseEntity(HttpStatus.NOT_FOUND)
        }
    }

    @GetMapping("/catalogs/{id}")
    fun getDatasetCatalogById(
        @RequestHeader(HttpHeaders.ACCEPT) accept: String?,
        @PathVariable id: String
    ): ResponseEntity<String> {
        val returnType = jenaLangFromAcceptHeader(accept)

        return if (returnType == Lang.RDFNULL) ResponseEntity(HttpStatus.NOT_ACCEPTABLE)
        else {
            datasetService.getDatasetCatalogById(id, returnType ?: Lang.TURTLE)
                ?.let { ResponseEntity(it, HttpStatus.OK) }
                ?: ResponseEntity(HttpStatus.NOT_FOUND)
        }
    }

    @GetMapping
    fun getAllDatasetCatalogs(@RequestHeader(HttpHeaders.ACCEPT) accept: String?): ResponseEntity<String> {
        val returnType = jenaLangFromAcceptHeader(accept)

        return if (returnType == Lang.RDFNULL) ResponseEntity(HttpStatus.NOT_ACCEPTABLE)
        else ResponseEntity(datasetService.getAllDatasetCatalogs(returnType ?: Lang.TURTLE), HttpStatus.OK)
    }

}
