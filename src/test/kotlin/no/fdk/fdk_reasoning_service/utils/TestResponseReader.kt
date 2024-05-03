package no.fdk.fdk_reasoning_service.utils

import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory

import java.io.InputStreamReader
import java.io.Reader
import java.io.StringReader
import java.nio.charset.StandardCharsets

class TestResponseReader {

    private fun resourceAsReader(resourceName: String): Reader {
        return InputStreamReader(javaClass.classLoader.getResourceAsStream(resourceName)!!, StandardCharsets.UTF_8)
    }

    fun readFile(filename: String): String =
        resourceAsReader(filename).readText()

    fun parseTurtleFile(filename: String, lang: String = "TURTLE"): Model {
        val expected = ModelFactory.createDefaultModel()
        expected.read(resourceAsReader(filename), "", lang)
        return expected
    }

    fun parseResponse(response: String, lang: String): Model {
        val responseModel = ModelFactory.createDefaultModel()
        responseModel.read(StringReader(response), "", lang)
        return responseModel
    }

    fun parseTurtleFiles(filenames: List<String>, lang: String = "Turtle"): Model {
        val m = ModelFactory.createDefaultModel()
        filenames.forEach {m.add(parseTurtleFile(it))}
        return m
    }
}
