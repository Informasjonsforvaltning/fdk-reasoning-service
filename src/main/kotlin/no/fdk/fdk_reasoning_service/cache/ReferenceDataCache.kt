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

    @Scheduled(fixedDelayString = "P3D")
    private fun invalidateAndUpdateLOS() {
        logger.info("updating LOS cache")
        try {
            with(RDFDataMgr.loadModel(uris.los, Lang.RDFXML)) {
                LOS.removeAll().add(this)
            }
        } catch (ex: Exception) {
            logger.error("Download failed for ${uris.los}", ex)
        }
    }

    private companion object {
        val ORGANIZATIONS: Model = ModelFactory.createDefaultModel()
        val LOS: Model = ModelFactory.createDefaultModel()
    }
}
