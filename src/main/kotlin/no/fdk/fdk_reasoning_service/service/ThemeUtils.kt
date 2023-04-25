package no.fdk.fdk_reasoning_service.service

import no.fdk.fdk_reasoning_service.model.ExternalRDFData
import no.fdk.fdk_reasoning_service.rdf.FDK
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.SimpleSelector
import org.apache.jena.rdf.model.Statement

private class ThemeTripleSelector: SimpleSelector() {
    override fun test(s: Statement): Boolean {
        return s.predicate == FDK.themePath
    }
}

fun ExternalRDFData.selectedThemeTriples(): Model =
    ModelFactory.createDefaultModel()
        .add(losData.listStatements(ThemeTripleSelector()))
        .add(eurovocs.listStatements(ThemeTripleSelector()))
        .add(dataThemes.listStatements(ThemeTripleSelector()))
