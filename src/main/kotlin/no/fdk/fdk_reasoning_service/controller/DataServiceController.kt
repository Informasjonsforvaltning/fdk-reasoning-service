package no.fdk.fdk_reasoning_service.controller

import no.fdk.fdk_reasoning_service.service.DataServiceService
import org.apache.jena.riot.Lang
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@CrossOrigin
@RequestMapping(
    value = ["/data-services"],
    produces = ["text/turtle", "text/n3", "application/rdf+json", "application/ld+json", "application/rdf+xml",
        "application/n-triples", "application/n-quads", "application/trig", "application/trix"]
)
class DataServiceController(private val dataServiceService: DataServiceService) {

    @GetMapping("/{id}")
    fun getDataServiceById(
        @RequestHeader(HttpHeaders.ACCEPT) accept: String?,
        @PathVariable id: String
    ): ResponseEntity<String> {
        val returnType = jenaLangFromAcceptHeader(accept)

        return if (returnType == Lang.RDFNULL) ResponseEntity(HttpStatus.NOT_ACCEPTABLE)
        else {
            dataServiceService.getDataServiceById(id, returnType ?: Lang.TURTLE)
                ?.let { ResponseEntity(it, HttpStatus.OK) }
                ?: ResponseEntity(HttpStatus.NOT_FOUND)
        }
    }

    @GetMapping("/catalogs/{id}")
    fun getDataServiceCatalogById(
        @RequestHeader(HttpHeaders.ACCEPT) accept: String?,
        @PathVariable id: String
    ): ResponseEntity<String> {
        val returnType = jenaLangFromAcceptHeader(accept)

        return if (returnType == Lang.RDFNULL) ResponseEntity(HttpStatus.NOT_ACCEPTABLE)
        else {
            dataServiceService.getDataServiceCatalogById(id, returnType ?: Lang.TURTLE)
                ?.let { ResponseEntity(it, HttpStatus.OK) }
                ?: ResponseEntity(HttpStatus.NOT_FOUND)
        }
    }

    @GetMapping
    fun getAllDataServiceCatalogs(@RequestHeader(HttpHeaders.ACCEPT) accept: String?): ResponseEntity<String> {
        val returnType = jenaLangFromAcceptHeader(accept)

        return if (returnType == Lang.RDFNULL) ResponseEntity(HttpStatus.NOT_ACCEPTABLE)
        else ResponseEntity(dataServiceService.getAllDataServiceCatalogs(returnType ?: Lang.TURTLE), HttpStatus.OK)
    }

}
