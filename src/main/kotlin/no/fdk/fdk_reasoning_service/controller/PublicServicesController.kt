package no.fdk.fdk_reasoning_service.controller

import no.fdk.fdk_reasoning_service.service.PublicServiceService
import org.apache.jena.riot.Lang
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@CrossOrigin
@RequestMapping(
    value = ["/public-services"],
    produces = ["text/turtle", "text/n3", "application/rdf+json", "application/ld+json", "application/rdf+xml",
        "application/n-triples", "application/n-quads", "application/trig", "application/trix"]
)
class PublicServicesController(private val publicServiceService: PublicServiceService) {

    @GetMapping("/{id}")
    fun getPublicServiceById(
        @RequestHeader(HttpHeaders.ACCEPT) accept: String?,
        @PathVariable id: String
    ): ResponseEntity<String> {
        val returnType = jenaLangFromAcceptHeader(accept)

        return if (returnType == Lang.RDFNULL) ResponseEntity(HttpStatus.NOT_ACCEPTABLE)
        else {
            publicServiceService.getPublicServiceById(id, returnType ?: Lang.TURTLE)
                ?.let { ResponseEntity(it, HttpStatus.OK) }
                ?: ResponseEntity(HttpStatus.NOT_FOUND)
        }
    }

    @GetMapping
    fun getAllPublicServices(@RequestHeader(HttpHeaders.ACCEPT) accept: String?): ResponseEntity<String> {
        val returnType = jenaLangFromAcceptHeader(accept)

        return if (returnType == Lang.RDFNULL) ResponseEntity(HttpStatus.NOT_ACCEPTABLE)
        else ResponseEntity(publicServiceService.getAllPublicServices(returnType ?: Lang.TURTLE), HttpStatus.OK)
    }

    @GetMapping("/catalogs/{id}")
    fun getCatalogById(
        @RequestHeader(HttpHeaders.ACCEPT) accept: String?,
        @PathVariable id: String
    ): ResponseEntity<String> {
        val returnType = jenaLangFromAcceptHeader(accept)

        return if (returnType == Lang.RDFNULL) ResponseEntity(HttpStatus.NOT_ACCEPTABLE)
        else {
            publicServiceService.getCatalogById(id, returnType ?: Lang.TURTLE)
                ?.let { ResponseEntity(it, HttpStatus.OK) }
                ?: ResponseEntity(HttpStatus.NOT_FOUND)
        }
    }

    @GetMapping("/catalogs")
    fun getAllCatalogs(@RequestHeader(HttpHeaders.ACCEPT) accept: String?): ResponseEntity<String> {
        val returnType = jenaLangFromAcceptHeader(accept)

        return if (returnType == Lang.RDFNULL) ResponseEntity(HttpStatus.NOT_ACCEPTABLE)
        else ResponseEntity(publicServiceService.getAllCatalogs(returnType ?: Lang.TURTLE), HttpStatus.OK)
    }

}
