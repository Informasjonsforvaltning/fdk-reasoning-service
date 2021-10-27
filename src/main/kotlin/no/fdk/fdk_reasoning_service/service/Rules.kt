package no.fdk.fdk_reasoning_service.service

val datasetRules = """
    @prefix dcat: <http://www.w3.org/ns/dcat#> .
    @prefix dct: <http://purl.org/dc/terms/> .
    @prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
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
"""
