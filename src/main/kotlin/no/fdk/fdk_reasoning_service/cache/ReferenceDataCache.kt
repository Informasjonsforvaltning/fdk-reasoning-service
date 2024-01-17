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
    fun conceptStatuses(): Model = CONCEPT_STATUSES
    fun conceptSubjects(): Model = CONCEPT_SUBJECTS
    fun ianaMediaTypes(): Model = MEDIA_TYPES
    fun fileTypes(): Model = FILE_TYPES
    fun openLicenses(): Model = OPEN_LICENSES
    fun linguisticSystems(): Model = LINGUISTIC_SYSTEMS
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

    @EventListener
    fun loadCacheOnStartup(event: ApplicationReadyEvent) {
        updateOrganizations()
        updateLOS()
        updateEUROVOC()
        updateDataThemes()
        updateConceptStatuses()
        updateConceptSubjects()
        updateMediaTypes()
        updateFileTypes()
        updateOpenLicenses()
        updateLinguisticSystems()
        updateLocations()
        updateAccessRights()
        updateFrequencies()
        updateProvenance()
        updatePublisherTypes()
        updateAdmsStatuses()
        updateRoleTypes()
        updateEvidenceTypes()
        updateChannelTypes()
        updateMainActivities()
        updateWeekDays()
    }

    @Scheduled(cron = "0 10 */3 * * ?")
    fun updateOrganizations() {
        try {
            with(RDFDataMgr.loadModel(uris.orgInternal, Lang.TURTLE)) {
                ORGANIZATIONS.removeAll().add(this)
            }
            logger.info("successfully updated organization cache")
        } catch (ex: Exception) {
            logger.error("Download failed for ${uris.orgInternal}", ex)
        }
    }

    @Scheduled(cron = "0 30 23 * * ?")
    fun updateLOS() {
        try {
            with(RDFDataMgr.loadModel(uris.los, Lang.TURTLE)) {
                LOS.removeAll().add(this)
            }
            logger.info("successfully updated LOS cache")
        } catch (ex: Exception) {
            logger.error("Download failed for ${uris.los}", ex)
        }
    }

    @Scheduled(cron = "0 15 23 * * ?")
    fun updateEUROVOC() {
        try {
            with(RDFDataMgr.loadModel(uris.eurovocs, Lang.TURTLE)) {
                EUROVOCS.removeAll().add(this)
            }
            logger.info("successfully updated EUROVOCS cache")
        } catch (ex: Exception) {
            logger.error("Download failed for ${uris.eurovocs}", ex)
        }
    }

    @Scheduled(cron = "0 40 23 * * ?")
    fun updateDataThemes() {
        try {
            with(RDFDataMgr.loadModel(uris.dataThemes, Lang.TURTLE)) {
                DATA_THEMES.removeAll().add(this)
            }
            logger.info("successfully updated data themes cache")
        } catch (ex: Exception) {
            logger.error("Download failed for ${uris.dataThemes}", ex)
        }
    }

    @Scheduled(cron = "0 45 23 * * ?")
    fun updateConceptStatuses() {
        try {
            with(RDFDataMgr.loadModel(uris.conceptStatuses, Lang.TURTLE)) {
                CONCEPT_STATUSES.removeAll().add(this)
            }
            logger.info("successfully updated concept status cache")
        } catch (ex: Exception) {
            logger.error("Download failed for ${uris.conceptStatuses}", ex)
        }
    }

    @Scheduled(cron = "0 50 * * * ?")
    fun updateConceptSubjects() {
        try {
            with(RDFDataMgr.loadModel(uris.conceptSubjects, Lang.TURTLE)) {
                CONCEPT_SUBJECTS.removeAll().add(this)
            }
            logger.info("successfully updated concept subjects cache")
        } catch (ex: Exception) {
            logger.error("Download failed for ${uris.conceptSubjects}", ex)
        }
    }

    @Scheduled(cron = "0 45 22 * * ?")
    fun updateMediaTypes() {
        try {
            with(RDFDataMgr.loadModel(uris.ianaMediaTypes, Lang.TURTLE)) {
                MEDIA_TYPES.removeAll().add(this)
            }
            logger.info("successfully updated IANA media types cache")
        } catch (ex: Exception) {
            logger.error("Download failed for ${uris.ianaMediaTypes}", ex)
        }
    }

    @Scheduled(cron = "0 40 22 * * ?")
    fun updateFileTypes() {
        try {
            with(RDFDataMgr.loadModel(uris.fileTypes, Lang.TURTLE)) {
                FILE_TYPES.removeAll().add(this)
            }
            logger.info("successfully updated EU file types cache")
        } catch (ex: Exception) {
            logger.error("Download failed for ${uris.fileTypes}", ex)
        }
    }

    @Scheduled(cron = "0 35 22 * * ?")
    fun updateOpenLicenses() {
        try {
            with(RDFDataMgr.loadModel(uris.openLicenses, Lang.TURTLE)) {
                OPEN_LICENSES.removeAll().add(this)
            }
            logger.info("successfully updated open licenses cache")
        } catch (ex: Exception) {
            logger.error("Download failed for ${uris.openLicenses}", ex)
        }
    }

    @Scheduled(cron = "0 30 22 * * ?")
    fun updateLinguisticSystems() {
        try {
            with(RDFDataMgr.loadModel(uris.linguisticSystems, Lang.TURTLE)) {
                LINGUISTIC_SYSTEMS.removeAll().add(this)
            }
            logger.info("successfully updated linguistic systems cache")
        } catch (ex: Exception) {
            logger.error("Download failed for ${uris.linguisticSystems}", ex)
        }
    }

    @Scheduled(cron = "0 15 22 * * ?")
    fun updateLocations() {
        try {
            val m = ModelFactory.createDefaultModel()
            with(RDFDataMgr.loadModel(uris.geonorgeNasjoner, Lang.TURTLE)) { m.add(this) }
            with(RDFDataMgr.loadModel(uris.geonorgeFylker, Lang.TURTLE)) { m.add(this) }
            with(RDFDataMgr.loadModel(uris.geonorgeKommuner, Lang.TURTLE)) { m.add(this) }
            LOCATIONS.removeAll().add(m)
            logger.info("successfully updated locations cache")
        } catch (ex: Exception) {
            logger.error("Update of locations failed", ex)
        }
    }

    @Scheduled(cron = "0 10 22 * * ?")
    fun updateAccessRights() {
        try {
            with(RDFDataMgr.loadModel(uris.accessRights, Lang.TURTLE)) {
                ACCESS_RIGHTS.removeAll().add(this)
            }
            logger.info("successfully updated access rights cache")
        } catch (ex: Exception) {
            logger.error("Download failed for ${uris.accessRights}", ex)
        }
    }

    @Scheduled(cron = "0 5 22 * * ?")
    fun updateFrequencies() {
        try {
            with(RDFDataMgr.loadModel(uris.frequencies, Lang.TURTLE)) {
                FREQUENCIES.removeAll().add(this)
            }
            logger.info("successfully updated frequencies cache")
        } catch (ex: Exception) {
            logger.error("Download failed for ${uris.frequencies}", ex)
        }
    }

    @Scheduled(cron = "0 50 21 * * ?")
    fun updateProvenance() {
        try {
            with(RDFDataMgr.loadModel(uris.provenance, Lang.TURTLE)) {
                PROVENANCE.removeAll().add(this)
            }
            logger.info("successfully updated provenance cache")
        } catch (ex: Exception) {
            logger.error("Download failed for ${uris.provenance}", ex)
        }
    }

    @Scheduled(cron = "0 45 21 * * ?")
    fun updatePublisherTypes() {
        try {
            with(RDFDataMgr.loadModel(uris.publisherTypes, Lang.TURTLE)) {
                PUBLISHER_TYPES.removeAll().add(this)
            }
            logger.info("successfully updated publisher types cache")
        } catch (ex: Exception) {
            logger.error("Download failed for ${uris.publisherTypes}", ex)
        }
    }

    @Scheduled(cron = "0 40 21 * * ?")
    fun updateAdmsStatuses() {
        try {
            with(RDFDataMgr.loadModel(uris.admsStatuses, Lang.TURTLE)) {
                ADMS_STATUSES.removeAll().add(this)
            }
            logger.info("successfully updated adms statuses cache")
        } catch (ex: Exception) {
            logger.error("Download failed for ${uris.admsStatuses}", ex)
        }
    }

    @Scheduled(cron = "0 35 21 * * ?")
    fun updateRoleTypes() {
        try {
            with(RDFDataMgr.loadModel(uris.roleTypes, Lang.TURTLE)) {
                ROLE_TYPES.removeAll().add(this)
            }
            logger.info("successfully updated role types cache")
        } catch (ex: Exception) {
            logger.error("Download failed for ${uris.roleTypes}", ex)
        }
    }

    @Scheduled(cron = "0 30 21 * * ?")
    fun updateEvidenceTypes() {
        try {
            with(RDFDataMgr.loadModel(uris.evidenceTypes, Lang.TURTLE)) {
                EVIDENCE_TYPES.removeAll().add(this)
            }
            logger.info("successfully updated evidence types cache")
        } catch (ex: Exception) {
            logger.error("Download failed for ${uris.evidenceTypes}", ex)
        }
    }

    @Scheduled(cron = "0 25 21 * * ?")
    fun updateChannelTypes() {
        try {
            with(RDFDataMgr.loadModel(uris.channelTypes, Lang.TURTLE)) {
                CHANNEL_TYPES.removeAll().add(this)
            }
            logger.info("successfully updated channel types cache")
        } catch (ex: Exception) {
            logger.error("Download failed for ${uris.channelTypes}", ex)
        }
    }

    @Scheduled(cron = "0 20 21 * * ?")
    fun updateMainActivities() {
        try {
            with(RDFDataMgr.loadModel(uris.mainActivities, Lang.TURTLE)) {
                MAIN_ACTIVITIES.removeAll().add(this)
            }
            logger.info("successfully updated main activities cache")
        } catch (ex: Exception) {
            logger.error("Download failed for ${uris.mainActivities}", ex)
        }
    }

    @Scheduled(cron = "0 15 21 * * ?")
    fun updateWeekDays() {
        try {
            with(RDFDataMgr.loadModel(uris.weekDays, Lang.TURTLE)) {
                WEEK_DAYS.removeAll().add(this)
            }
            logger.info("successfully updated week days cache")
        } catch (ex: Exception) {
            logger.error("Download failed for ${uris.weekDays}", ex)
        }
    }

    private companion object {
        val ORGANIZATIONS: Model = ModelFactory.createDefaultModel()
        val LOS: Model = ModelFactory.createDefaultModel()
        val EUROVOCS: Model = ModelFactory.createDefaultModel()
        val DATA_THEMES: Model = ModelFactory.createDefaultModel()
        val CONCEPT_STATUSES: Model = ModelFactory.createDefaultModel()
        val CONCEPT_SUBJECTS: Model = ModelFactory.createDefaultModel()
        val MEDIA_TYPES: Model = ModelFactory.createDefaultModel()
        val FILE_TYPES: Model = ModelFactory.createDefaultModel()
        val OPEN_LICENSES: Model = ModelFactory.createDefaultModel()
        val LINGUISTIC_SYSTEMS: Model = ModelFactory.createDefaultModel()
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
    }
}
