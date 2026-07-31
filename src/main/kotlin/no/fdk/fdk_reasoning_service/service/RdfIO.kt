package no.fdk.fdk_reasoning_service.service

import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.riot.Lang
import java.io.ByteArrayOutputStream
import java.io.StringReader

fun Model.createRDFResponse(responseType: Lang): String =
    ByteArrayOutputStream().use { out ->
        write(out, responseType.name)
        out.flush()
        out.toString("UTF-8")
    }

fun parseRDFResponse(responseBody: String, rdfLanguage: Lang): Model {
    val responseModel = ModelFactory.createDefaultModel()
    responseModel.read(StringReader(responseBody), "", rdfLanguage.name)
    return responseModel
}
