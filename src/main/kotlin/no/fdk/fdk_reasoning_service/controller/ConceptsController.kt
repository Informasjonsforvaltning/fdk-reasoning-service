package no.fdk.fdk_reasoning_service.controller

import no.fdk.fdk_reasoning_service.service.ConceptService
import org.apache.jena.riot.Lang
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@CrossOrigin
@RequestMapping(
    value = ["/concepts"],
    produces = ["text/turtle", "text/n3", "application/rdf+json", "application/ld+json", "application/rdf+xml",
        "application/n-triples", "application/n-quads", "application/trig", "application/trix"]
)
class ConceptsController(private val conceptService: ConceptService) {

    @GetMapping("/{id}")
    fun getConceptById(
        @RequestHeader(HttpHeaders.ACCEPT) accept: String?,
        @PathVariable id: String
    ): ResponseEntity<String> {
        val returnType = jenaLangFromAcceptHeader(accept)

        return if (returnType == Lang.RDFNULL) ResponseEntity(HttpStatus.NOT_ACCEPTABLE)
        else {
            conceptService.getConceptById(id, returnType ?: Lang.TURTLE)
                ?.let { ResponseEntity(it, HttpStatus.OK) }
                ?: ResponseEntity(HttpStatus.NOT_FOUND)
        }
    }

    @GetMapping("/collections/{id}")
    fun getConceptCollectionById(
        @RequestHeader(HttpHeaders.ACCEPT) accept: String?,
        @PathVariable id: String
    ): ResponseEntity<String> {
        val returnType = jenaLangFromAcceptHeader(accept)

        return if (returnType == Lang.RDFNULL) ResponseEntity(HttpStatus.NOT_ACCEPTABLE)
        else {
            conceptService.getConceptCollectionById(id, returnType ?: Lang.TURTLE)
                ?.let { ResponseEntity(it, HttpStatus.OK) }
                ?: ResponseEntity(HttpStatus.NOT_FOUND)
        }
    }

    @GetMapping
    fun getAllConceptCollections(@RequestHeader(HttpHeaders.ACCEPT) accept: String?): ResponseEntity<String> {
        val returnType = jenaLangFromAcceptHeader(accept)

        return if (returnType == Lang.RDFNULL) ResponseEntity(HttpStatus.NOT_ACCEPTABLE)
        else ResponseEntity(conceptService.getAllConceptCollections(returnType ?: Lang.TURTLE), HttpStatus.OK)
    }

}
