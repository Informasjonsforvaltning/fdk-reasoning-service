package no.fdk.fdk_reasoning_service.service

val datasetRules = """
    @prefix dcat: <http://www.w3.org/ns/dcat#> .
    @prefix dct: <http://purl.org/dc/terms/> .
    @prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
    @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
    @prefix fdk: <https://raw.githubusercontent.com/Informasjonsforvaltning/fdk-reasoning-service/main/src/main/resources/ontology/fdk.owl#> .

    [seriesIsDataset:
        (?series rdf:type dcat:DatasetSeries),
        -> (?series rdf:type dcat:Dataset) ]

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

    [catalogPublisherWhenMissing:
        (?dataset rdf:type dcat:Dataset),
        (?catalog dcat:dataset ?dataset),
        (?catalog dct:publisher ?catalogPublisher),
        noValue(?dataset dct:publisher)
        -> (?dataset dct:publisher ?catalogPublisher)
    ]

    [seriesPublisherWhenMissing:
        (?dataset rdf:type dcat:Dataset),
        (?dataset dcat:inSeries ?series),
        (?series dct:publisher ?seriesPublisher),
        noValue(?dataset dct:publisher)
        -> (?dataset dct:publisher ?seriesPublisher)
    ]
"""

const val dataServiceRules = """
    @prefix dcat: <http://www.w3.org/ns/dcat#> .
    @prefix dct: <http://purl.org/dc/terms/> .

    [catalogPublisherWhenMissing:
        (?service rdf:type dcat:DataService),
        (?catalog dcat:service ?service),
        (?catalog dct:publisher ?catalogPublisher),
        noValue(?service dct:publisher)
        -> (?service dct:publisher ?catalogPublisher)
    ]
"""

const val infoModelRules = """
    @prefix modelldcatno: <https://data.norge.no/vocabulary/modelldcatno#> .
    @prefix dct: <http://purl.org/dc/terms/> .

    [catalogPublisherWhenMissing:
        (?model rdf:type modelldcatno:InformationModel),
        (?catalog modelldcatno:model ?model),
        (?catalog dct:publisher ?catalogPublisher),
        noValue(?model dct:publisher)
        -> (?model dct:publisher ?catalogPublisher)
    ]
"""

const val conceptRules = """
    @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
    @prefix dct: <http://purl.org/dc/terms/> .

    [collectionPublisherWhenMissing:
        (?concept rdf:type skos:Concept),
        (?collection skos:member ?concept),
        (?collection dct:publisher ?collectionPublisher),
        noValue(?concept dct:publisher)
        -> (?concept dct:publisher ?collectionPublisher)
    ]
"""

const val serviceRules = """
    @prefix cv: <http://data.europa.eu/m8g/> .

    [extendHasParticipantWhenMissing:
        (?participation rdf:type cv:Participation),
        (?agent cv:participates ?participation)
        noValue(?participation cv:hasParticipant)
        -> (?participation cv:hasParticipant ?agent)
    ]
"""

const val addThemeTitles = """
    @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
    @prefix dct: <http://purl.org/dc/terms/> .
    @prefix dcat:  <http://www.w3.org/ns/dcat#> .
    @prefix fdk: <https://raw.githubusercontent.com/Informasjonsforvaltning/fdk-reasoning-service/main/src/main/resources/ontology/fdk.owl#> .

    [themeTitleToLabelWhenMissing:
        (?subject dcat:theme ?theme),
        (?theme dct:title ?themeTitle),
        (?theme fdk:missingLabel ?missingLabel),
        equal(?missingLabel 'true'^^xsd:boolean)
        -> (?theme skos:prefLabel ?themeTitle)
    ]
"""

const val tagThemesMissingLabel = """
    @prefix dcat:  <http://www.w3.org/ns/dcat#> .
    @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
    @prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
    @prefix fdk: <https://raw.githubusercontent.com/Informasjonsforvaltning/fdk-reasoning-service/main/src/main/resources/ontology/fdk.owl#> .

    [themeTitleToLabelWhenMissing:
        (?subject dcat:theme ?theme),
        noValue(?theme skos:prefLabel)
        -> (?theme fdk:missingLabel 'true'^^xsd:boolean)
    ]
"""

const val labelToTitle = """
    @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
    @prefix dct: <http://purl.org/dc/terms/> .

    [prefLabelToTitle:
        (?subject skos:prefLabel ?prefLabel),
        -> (?subject dct:title ?prefLabel)
    ]
"""

private fun openDataURIVariants(uriBase: String): List<String> =
    listOf(
        "http://$uriBase",
        "http://$uriBase/",
        "https://$uriBase",
        "https://$uriBase/"
    )
