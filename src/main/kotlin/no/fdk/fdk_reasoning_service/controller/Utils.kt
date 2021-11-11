package no.fdk.fdk_reasoning_service.controller

import org.apache.jena.riot.Lang

fun jenaLangFromAcceptHeader(accept: String?): Lang? =
    when {
        accept == null -> null
        accept.contains(Lang.TURTLE.headerString) -> Lang.TURTLE
        accept.contains("text/n3") -> Lang.N3
        accept.contains(Lang.TRIG.headerString) -> Lang.TRIG
        accept.contains(Lang.RDFXML.headerString) -> Lang.RDFXML
        accept.contains(Lang.RDFJSON.headerString) -> Lang.RDFJSON
        accept.contains(Lang.JSONLD.headerString) -> Lang.JSONLD
        accept.contains(Lang.NTRIPLES.headerString) -> Lang.NTRIPLES
        accept.contains(Lang.NQUADS.headerString) -> Lang.NQUADS
        accept.contains(Lang.TRIX.headerString) -> Lang.TRIX
        accept.contains("*/*") -> null
        else -> Lang.RDFNULL
    }
