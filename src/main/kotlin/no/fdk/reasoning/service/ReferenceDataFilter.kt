package no.fdk.reasoning.service

import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.Resource

fun modelOfContainedReferenceData(inputModel: Model, referenceDataModel: Model): Model {
    val m = ModelFactory.createDefaultModel()

    referenceDataModel
        .listSubjects()
        .toList()
        .filter { inputModel.containsTriple("?s", "?p", "<${it.uri}>") }
        .forEach { it.recursiveAddReferenceCodeProperties(m) }

    return m
}

private fun Resource.recursiveAddReferenceCodeProperties(m: Model) {
    listProperties()
        .toList()
        .filter { !m.contains(it) }
        .also { m.add(it) }
        .filter { it.isResourceProperty() }
        .map { it.resource }
        .filter { it.isAnon }
        .forEach { it.recursiveAddReferenceCodeProperties(m) }
}
