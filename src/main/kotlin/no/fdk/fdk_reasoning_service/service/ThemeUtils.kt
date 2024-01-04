package no.fdk.fdk_reasoning_service.service

import no.fdk.fdk_reasoning_service.model.ExternalRDFData
import no.fdk.fdk_reasoning_service.rdf.FDK
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory

fun ExternalRDFData.selectedThemeTriples(): Model =
    ModelFactory.createDefaultModel()
        .add(losData.listStatements().filterKeep { s -> s.predicate == FDK.themePath }.toList())
        .add(eurovocs.listStatements().filterKeep { s -> s.predicate == FDK.themePath }.toList())
        .add(dataThemes.listStatements().filterKeep { s -> s.predicate == FDK.themePath }.toList())
