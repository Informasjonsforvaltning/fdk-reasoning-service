package no.fdk.fdk_reasoning_service.cache

import no.fdk.fdk_reasoning_service.config.ApplicationURI
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.riot.Lang
import org.apache.jena.riot.RDFDataMgr
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

private val logger: Logger = LoggerFactory.getLogger(ReferenceDataCache::class.java)

@Service
class ReferenceDataCache(private val uris: ApplicationURI) {

    fun organizations(): Model {
        return ModelFactory.createDefaultModel().add(ORGANIZATIONS)
    }

    fun los(): Model {
        return ModelFactory.createDefaultModel().add(LOS)
    }

    fun eurovocs(): Model {
        return ModelFactory.createDefaultModel().add(EUROVOCS)
    }

    fun dataThemes(): Model {
        return ModelFactory.createDefaultModel().add(DATA_THEMES)
    }

    fun conceptStatuses(): Model {
        return ModelFactory.createDefaultModel().add(CONCEPT_STATUSES)
    }

    fun conceptSubjects(): Model {
        return ModelFactory.createDefaultModel().add(CONCEPT_SUBJECTS)
    }

    @Scheduled(fixedDelayString = "PT3H")
    private fun invalidateAndUpdateOrganizations() {
        logger.info("updating organization cache")
        try {
            with(RDFDataMgr.loadModel(uris.orgInternal, Lang.TURTLE)) {
                ORGANIZATIONS.removeAll().add(this)
            }
        } catch (ex: Exception) {
            logger.error("Download failed for ${uris.orgInternal}", ex)
        }
    }

    @Scheduled(fixedDelayString = "PT5H")
    private fun invalidateAndUpdateLOS() {
        logger.info("updating LOS cache")
        try {
            with(RDFDataMgr.loadModel(uris.los, Lang.TURTLE)) {
                LOS.removeAll().add(this)
            }
        } catch (ex: Exception) {
            logger.error("Download failed for ${uris.los}", ex)
        }
    }

    @Scheduled(fixedDelayString = "PT23H")
    private fun invalidateAndUpdateEUROVOC() {
        logger.info("updating EUROVOCS cache")
        try {
            with(RDFDataMgr.loadModel(uris.eurovocs, Lang.TURTLE)) {
                EUROVOCS.removeAll().add(this)
            }
        } catch (ex: Exception) {
            logger.error("Download failed for ${uris.eurovocs}", ex)
        }
    }

    @Scheduled(fixedDelayString = "PT22H")
    private fun invalidateAndUpdateDataThemes() {
        logger.info("updating data themes cache")
        try {
            with(RDFDataMgr.loadModel(uris.dataThemes, Lang.TURTLE)) {
                DATA_THEMES.removeAll().add(this)
            }
        } catch (ex: Exception) {
            logger.error("Download failed for ${uris.dataThemes}", ex)
        }
    }

    @Scheduled(fixedDelayString = "PT21H")
    private fun invalidateAndUpdateConceptStatuses() {
        logger.info("updating concept status cache")
        try {
            with(RDFDataMgr.loadModel(uris.conceptStatuses, Lang.TURTLE)) {
                CONCEPT_STATUSES.removeAll().add(this)
            }
        } catch (ex: Exception) {
            logger.error("Download failed for ${uris.conceptStatuses}", ex)
        }
    }

    @Scheduled(cron = "0 50 * * * ?")
    private fun invalidateAndUpdateConceptSubjects() {
        logger.info("updating concept subjects cache")
        try {
            with(RDFDataMgr.loadModel(uris.conceptSubjects, Lang.TURTLE)) {
                CONCEPT_SUBJECTS.removeAll().add(this)
            }
        } catch (ex: Exception) {
            logger.error("Download failed for ${uris.conceptSubjects}", ex)
        }
    }

    private companion object {
        val ORGANIZATIONS: Model = ModelFactory.createDefaultModel()
        val LOS: Model = ModelFactory.createDefaultModel()
        val EUROVOCS: Model = ModelFactory.createDefaultModel()
        val DATA_THEMES: Model = ModelFactory.createDefaultModel()
        val CONCEPT_STATUSES: Model = ModelFactory.createDefaultModel()
        val CONCEPT_SUBJECTS: Model = ModelFactory.createDefaultModel()
    }
}
