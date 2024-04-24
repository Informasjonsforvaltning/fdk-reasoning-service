package no.fdk.fdk_reasoning_service.utils

import no.fdk.fdk_reasoning_service.model.ExternalRDFData

private val responseReader = TestResponseReader()

val RDF_DATA = ExternalRDFData(
    orgData = responseReader.parseTurtleFile("orgs.ttl"),
    losData = responseReader.parseTurtleFile("los.ttl", "TURTLE"),
    eurovocs = responseReader.parseTurtleFile("eurovocs.ttl", "TURTLE"),
    dataThemes = responseReader.parseTurtleFile("data_themes.ttl", "TURTLE"),
    conceptStatuses = responseReader.parseTurtleFile("concept_statuses.ttl", "TURTLE"),
    conceptSubjects = responseReader.parseTurtleFile("concept_subjects.ttl", "TURTLE"),
    ianaMediaTypes = responseReader.parseTurtleFile("media_types.ttl", "TURTLE"),
    fileTypes = responseReader.parseTurtleFile("file_types.ttl", "TURTLE"),
    openLicenses = responseReader.parseTurtleFile("open_licenses.ttl", "TURTLE"),
    linguisticSystems = responseReader.parseTurtleFile("linguistic_systems.ttl", "TURTLE"),
    accessRights = responseReader.parseTurtleFile("access_rights.ttl", "TURTLE"),
    frequencies = responseReader.parseTurtleFile("frequencies.ttl", "TURTLE"),
    provenance = responseReader.parseTurtleFile("provenance_statements.ttl", "TURTLE"),
    publisherTypes = responseReader.parseTurtleFile("publisher_types.ttl", "TURTLE"),
    admsStatuses = responseReader.parseTurtleFile("adms_statuses.ttl", "TURTLE"),
    roleTypes = responseReader.parseTurtleFile("role_types.ttl", "TURTLE"),
    evidenceTypes = responseReader.parseTurtleFile("evidence_types.ttl", "TURTLE"),
    channelTypes = responseReader.parseTurtleFile("channel_types.ttl", "TURTLE"),
    mainActivities = responseReader.parseTurtleFile("main_activities.ttl", "TURTLE"),
    weekDays = responseReader.parseTurtleFile("week_days.ttl", "TURTLE"),
    locations = responseReader.parseTurtleFile("nasjoner.ttl", "TURTLE")
        .union(responseReader.parseTurtleFile("fylker.ttl", "TURTLE"))
        .union(responseReader.parseTurtleFile("kommuner.ttl", "TURTLE")))
