package no.fdk.fdk_reasoning_service.service

val datasetRules = """
    @prefix dcat: <http://www.w3.org/ns/dcat#> .
    @prefix dct: <http://purl.org/dc/terms/> .
    @prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
    @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
    @prefix fdk: <https://raw.githubusercontent.com/Informasjonsforvaltning/fdk-reasoning-service/master/src/main/resources/ontology/fdk.owl#> .

    [isAuthoritative:
        (?dataset rdf:type dcat:Dataset),
        (?dataset dct:provenance ?provenance),
        equal(?provenance, <http://data.brreg.no/datakatalog/provinens/nasjonal>)
        -> (?dataset fdk:isAuthoritative 'true'^^xsd:boolean) ]

    [isRelatedToTransportportal:
        (?dataset rdf:type dcat:Dataset),
        (?dataset dct:accessRights ?rights),
        equal(?rights, <http://publications.europa.eu/resource/authority/access-right/PUBLIC>),
        (?dataset dcat:theme ?theme),
        strConcat(?theme,?themeStr),
        regex(?themeStr, '${napThemes.joinToString(separator = "|")}')
        -> (?dataset fdk:isRelatedToTransportportal 'true'^^xsd:boolean)
    ]

    [isOpenData-license:
        (?dataset rdf:type dcat:Dataset),
        (?dataset dct:accessRights ?rights),
        equal(?rights, <http://publications.europa.eu/resource/authority/access-right/PUBLIC>),
        (?dataset dcat:distribution ?distribution),
        (?distribution dct:license ?license),
        strConcat(?license,?licenseStr),
        regex(?licenseStr, '${openDataURIBases.flatMap { openDataURIVariants(it) }.joinToString(separator = "|")}')
        -> (?dataset fdk:isOpenData 'true'^^xsd:boolean)
    ]

    [isOpenData-source:
        (?dataset rdf:type dcat:Dataset),
        (?dataset dct:accessRights ?rights),
        equal(?rights, <http://publications.europa.eu/resource/authority/access-right/PUBLIC>),
        (?dataset dcat:distribution ?distribution),
        (?distribution dct:license ?license),
        (?license dct:source ?source),
        strConcat(?source,?sourceStr),
        regex(?sourceStr, '${openDataURIBases.flatMap { openDataURIVariants(it) }.joinToString(separator = "|")}')
        -> (?dataset fdk:isOpenData 'true'^^xsd:boolean)
    ]

    [exactMatch:
        (?dataset rdf:type dcat:Dataset),
        (?dataset dcat:theme ?theme),
        (?theme skos:exactMatch ?exactMatch)
        -> (?dataset dcat:theme ?exactMatch)
    ]

    [closeMatch:
        (?dataset rdf:type dcat:Dataset),
        (?dataset dcat:theme ?theme),
        (?theme skos:closeMatch ?exactMatch)
        -> (?dataset dcat:theme ?exactMatch)
    ]

    [broadMatch:
        (?dataset rdf:type dcat:Dataset),
        (?dataset dcat:theme ?theme),
        (?theme skos:broadMatch ?exactMatch)
        -> (?dataset dcat:theme ?exactMatch)
    ]
"""

private fun openDataURIVariants(uriBase: String): List<String> =
    listOf(
        "http://$uriBase",
        "http://$uriBase/",
        "https://$uriBase",
        "https://$uriBase/"
    )
