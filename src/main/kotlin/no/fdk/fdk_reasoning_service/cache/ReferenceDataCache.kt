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
    }
}
