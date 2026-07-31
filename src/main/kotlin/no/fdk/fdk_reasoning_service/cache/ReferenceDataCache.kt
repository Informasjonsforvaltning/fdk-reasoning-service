package no.fdk.fdk_reasoning_service.cache

import no.fdk.fdk_reasoning_service.config.ApplicationURI
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.riot.Lang
import org.apache.jena.riot.RDFDataMgr
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

private val logger: Logger = LoggerFactory.getLogger(ReferenceDataCache::class.java)

@Service
class ReferenceDataCache(private val uris: ApplicationURI) {

    fun organizations(): Model = ORGANIZATIONS
    fun los(): Model = LOS
    fun eurovocs(): Model = EUROVOCS
    fun dataThemes(): Model = DATA_THEMES
    fun mobilityThemes(): Model = MOBILITY_THEMES
    fun conceptStatuses(): Model = CONCEPT_STATUSES
    fun conceptSubjects(): Model = CONCEPT_SUBJECTS
    fun ianaMediaTypes(): Model = MEDIA_TYPES
    fun fileTypes(): Model = FILE_TYPES
    fun licences(): Model = LICENCES
    fun languages(): Model = LANGUAGES
    fun locations(): Model = LOCATIONS
    fun accessRights(): Model = ACCESS_RIGHTS
    fun frequencies(): Model = FREQUENCIES
    fun provenance(): Model = PROVENANCE
    fun publisherTypes(): Model = PUBLISHER_TYPES
    fun admsStatuses(): Model = ADMS_STATUSES
    fun roleTypes(): Model = ROLE_TYPES
    fun evidenceTypes(): Model = EVIDENCE_TYPES
    fun channelTypes(): Model = CHANNEL_TYPES
    fun mainActivities(): Model = MAIN_ACTIVITIES
    fun weekDays(): Model = WEEK_DAYS
    fun datasetTypes(): Model = DATASET_TYPES
    fun distributionStatuses(): Model = DISTRIBUTION_STATUSES
    fun mobilityDataStandards(): Model = MOBILITY_DATA_STANDARDS
    fun mobilityConditions(): Model = MOBILITY_CONDITIONS
    fun highValueCategories(): Model = HIGH_VALUE_CATEGORIES
    fun qualityDimensions(): Model = QUALITY_DIMENSIONS
    fun legalResourceTypes(): Model = LEGAL_RESOURCE_TYPES
    fun geonames(): Model = GEONAMES
    fun euContinents(): Model = EU_CONTINENTS
    fun euCountries(): Model = EU_COUNTRIES

    private val startupUpdates: List<() -> Unit> = listOf(
        ::updateOrganizations,
        ::updateLOS,
        ::updateEUROVOC,
        ::updateDataThemes,
        ::updateMobilityThemes,
        ::updateConceptStatuses,
        ::updateConceptSubjects,
        ::updateMediaTypes,
        ::updateFileTypes,
        ::updateLicences,
        ::updateLanguages,
        ::updateLocations,
        ::updateAccessRights,
        ::updateFrequencies,
        ::updateProvenance,
        ::updatePublisherTypes,
        ::updateAdmsStatuses,
        ::updateRoleTypes,
        ::updateEvidenceTypes,
        ::updateChannelTypes,
        ::updateMainActivities,
        ::updateWeekDays,
        ::updateDatasetTypes,
        ::updateDistributionStatuses,
        ::updateMobilityDataStandards,
        ::updateMobilityConditions,
        ::updateHighValueCategories,
        ::updateQualityDimensions,
        ::updateLegalResourceTypes,
        ::updateGeonames,
        ::updateEuContinents,
        ::updateEuCountries,
    )

    @EventListener
    fun loadCacheOnStartup(event: ApplicationReadyEvent) {
        startupUpdates.forEach { it() }
    }

    private fun refresh(
        label: String,
        url: String,
        target: Model,
        errorMessage: String = "Download failed for $url",
    ) {
        try {
            with(RDFDataMgr.loadModel(url, Lang.TURTLE)) {
                target.removeAll().add(this)
            }
            logger.debug("successfully updated $label cache")
        } catch (ex: Exception) {
            logger.error(errorMessage, ex)
        }
    }

    @Scheduled(cron = "0 10 */3 * * ?")
    fun updateOrganizations() =
        refresh("organization", uris.orgInternal, ORGANIZATIONS)

    @Scheduled(cron = "0 30 23 * * ?")
    fun updateLOS() =
        refresh("LOS", uris.los, LOS)

    @Scheduled(cron = "0 15 23 * * ?")
    fun updateEUROVOC() =
        refresh("EUROVOCS", uris.eurovocs, EUROVOCS)

    @Scheduled(cron = "0 40 23 * * ?")
    fun updateDataThemes() =
        refresh("data themes", uris.dataThemes, DATA_THEMES)

    @Scheduled(cron = "0 42 23 * * ?")
    fun updateMobilityThemes() =
        refresh("mobility themes", uris.mobilityThemes, MOBILITY_THEMES)

    @Scheduled(cron = "0 45 23 * * ?")
    fun updateConceptStatuses() =
        refresh("concept status", uris.conceptStatuses, CONCEPT_STATUSES)

    @Scheduled(cron = "0 50 * * * ?")
    fun updateConceptSubjects() =
        refresh("concept subjects", uris.conceptSubjects, CONCEPT_SUBJECTS)

    @Scheduled(cron = "0 45 22 * * ?")
    fun updateMediaTypes() =
        refresh("IANA media types", uris.ianaMediaTypes, MEDIA_TYPES)

    @Scheduled(cron = "0 40 22 * * ?")
    fun updateFileTypes() =
        refresh("EU file types", uris.fileTypes, FILE_TYPES)

    @Scheduled(cron = "0 35 22 * * ?")
    fun updateLicences() =
        refresh("licences", uris.licences, LICENCES)

    @Scheduled(cron = "0 30 22 * * ?")
    fun updateLanguages() =
        refresh("languages", uris.languages, LANGUAGES)

    @Scheduled(cron = "0 15 22 * * ?")
    fun updateLocations() =
        refresh(
            "locations",
            uris.administrativeEnheter,
            LOCATIONS,
            errorMessage = "Update of locations failed",
        )

    @Scheduled(cron = "0 10 22 * * ?")
    fun updateAccessRights() =
        refresh("access rights", uris.accessRights, ACCESS_RIGHTS)

    @Scheduled(cron = "0 5 22 * * ?")
    fun updateFrequencies() =
        refresh("frequencies", uris.frequencies, FREQUENCIES)

    @Scheduled(cron = "0 50 21 * * ?")
    fun updateProvenance() =
        refresh("provenance", uris.provenance, PROVENANCE)

    @Scheduled(cron = "0 45 21 * * ?")
    fun updatePublisherTypes() =
        refresh("publisher types", uris.publisherTypes, PUBLISHER_TYPES)

    @Scheduled(cron = "0 40 21 * * ?")
    fun updateAdmsStatuses() =
        refresh("adms statuses", uris.admsStatuses, ADMS_STATUSES)

    @Scheduled(cron = "0 35 21 * * ?")
    fun updateRoleTypes() =
        refresh("role types", uris.roleTypes, ROLE_TYPES)

    @Scheduled(cron = "0 30 21 * * ?")
    fun updateEvidenceTypes() =
        refresh("evidence types", uris.evidenceTypes, EVIDENCE_TYPES)

    @Scheduled(cron = "0 25 21 * * ?")
    fun updateChannelTypes() =
        refresh("channel types", uris.channelTypes, CHANNEL_TYPES)

    @Scheduled(cron = "0 20 21 * * ?")
    fun updateMainActivities() =
        refresh("main activities", uris.mainActivities, MAIN_ACTIVITIES)

    @Scheduled(cron = "0 15 21 * * ?")
    fun updateWeekDays() =
        refresh("week days", uris.weekDays, WEEK_DAYS)

    @Scheduled(cron = "0 10 21 * * ?")
    fun updateDatasetTypes() =
        refresh("dataset types", uris.datasetTypes, DATASET_TYPES)

    @Scheduled(cron = "0 05 21 * * ?")
    fun updateDistributionStatuses() =
        refresh("distribution statuses", uris.distributionStatuses, DISTRIBUTION_STATUSES)

    @Scheduled(cron = "0 0 21 * * ?")
    fun updateMobilityDataStandards() =
        refresh("mobility data standards", uris.mobilityDataStandards, MOBILITY_DATA_STANDARDS)

    @Scheduled(cron = "0 55 20 * * ?")
    fun updateMobilityConditions() =
        refresh("mobility conditions", uris.mobilityConditions, MOBILITY_CONDITIONS)

    @Scheduled(cron = "0 50 20 * * ?")
    fun updateHighValueCategories() =
        refresh("high value categories", uris.highValueCategories, HIGH_VALUE_CATEGORIES)

    @Scheduled(cron = "0 45 20 * * ?")
    fun updateQualityDimensions() =
        refresh("quality dimensions", uris.qualityDimensions, QUALITY_DIMENSIONS)

    @Scheduled(cron = "0 40 20 * * ?")
    fun updateLegalResourceTypes() =
        refresh("legal resource types", uris.legalResourceTypes, LEGAL_RESOURCE_TYPES)

    @Scheduled(cron = "0 35 20 * * ?")
    fun updateGeonames() =
        refresh("geonames", uris.geonames, GEONAMES)

    @Scheduled(cron = "0 30 20 * * ?")
    fun updateEuContinents() =
        refresh("EU continents", uris.euContinents, EU_CONTINENTS)

    @Scheduled(cron = "0 25 20 * * ?")
    fun updateEuCountries() =
        refresh("EU countries", uris.euCountries, EU_COUNTRIES)

    private companion object {
        val ORGANIZATIONS: Model = ModelFactory.createDefaultModel()
        val LOS: Model = ModelFactory.createDefaultModel()
        val EUROVOCS: Model = ModelFactory.createDefaultModel()
        val DATA_THEMES: Model = ModelFactory.createDefaultModel()
        val MOBILITY_THEMES: Model = ModelFactory.createDefaultModel()
        val CONCEPT_STATUSES: Model = ModelFactory.createDefaultModel()
        val CONCEPT_SUBJECTS: Model = ModelFactory.createDefaultModel()
        val MEDIA_TYPES: Model = ModelFactory.createDefaultModel()
        val FILE_TYPES: Model = ModelFactory.createDefaultModel()
        val LICENCES: Model = ModelFactory.createDefaultModel()
        val LANGUAGES: Model = ModelFactory.createDefaultModel()
        val LOCATIONS: Model = ModelFactory.createDefaultModel()
        val ACCESS_RIGHTS: Model = ModelFactory.createDefaultModel()
        val FREQUENCIES: Model = ModelFactory.createDefaultModel()
        val PROVENANCE: Model = ModelFactory.createDefaultModel()
        val PUBLISHER_TYPES: Model = ModelFactory.createDefaultModel()
        val ADMS_STATUSES: Model = ModelFactory.createDefaultModel()
        val ROLE_TYPES: Model = ModelFactory.createDefaultModel()
        val EVIDENCE_TYPES: Model = ModelFactory.createDefaultModel()
        val CHANNEL_TYPES: Model = ModelFactory.createDefaultModel()
        val MAIN_ACTIVITIES: Model = ModelFactory.createDefaultModel()
        val WEEK_DAYS: Model = ModelFactory.createDefaultModel()
        val DATASET_TYPES: Model = ModelFactory.createDefaultModel()
        val DISTRIBUTION_STATUSES: Model = ModelFactory.createDefaultModel()
        val MOBILITY_DATA_STANDARDS: Model = ModelFactory.createDefaultModel()
        val MOBILITY_CONDITIONS: Model = ModelFactory.createDefaultModel()
        val HIGH_VALUE_CATEGORIES: Model = ModelFactory.createDefaultModel()
        val QUALITY_DIMENSIONS: Model = ModelFactory.createDefaultModel()
        val LEGAL_RESOURCE_TYPES: Model = ModelFactory.createDefaultModel()
        val GEONAMES: Model = ModelFactory.createDefaultModel()
        val EU_CONTINENTS: Model = ModelFactory.createDefaultModel()
        val EU_COUNTRIES: Model = ModelFactory.createDefaultModel()
    }
}
