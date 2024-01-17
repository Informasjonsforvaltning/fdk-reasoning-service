package no.fdk.fdk_reasoning_service.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("application.uri")
data class ApplicationURI(
    val orgExternal: String,
    val orgInternal: String,
    val los: String,
    val eurovocs: String,
    val dataThemes: String,
    val conceptStatuses: String,
    val conceptSubjects: String,
    val ianaMediaTypes: String,
    val fileTypes: String,
    val openLicenses: String,
    val linguisticSystems: String,
    val geonorgeNasjoner: String,
    val geonorgeFylker: String,
    val geonorgeKommuner: String,
    val accessRights: String,
    val frequencies: String,
    val provenance: String,
    val publisherTypes: String,
    val admsStatuses: String,
    val roleTypes: String,
    val evidenceTypes: String,
    val channelTypes: String,
    val mainActivities: String,
    val weekDays: String
)
