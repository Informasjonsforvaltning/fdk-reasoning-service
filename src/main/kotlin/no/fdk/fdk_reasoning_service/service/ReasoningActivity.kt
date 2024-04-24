package no.fdk.fdk_reasoning_service.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import no.fdk.fdk_reasoning_service.cache.ReferenceDataCache
import no.fdk.fdk_reasoning_service.model.CatalogType
import no.fdk.fdk_reasoning_service.model.ExternalRDFData
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.*

private val LOGGER: Logger = LoggerFactory.getLogger(ReasoningActivity::class.java)

@Component
class ReasoningActivity(
    private val conceptService: ConceptService,
    private val referenceDataCache: ReferenceDataCache
) : CoroutineScope by CoroutineScope(Dispatchers.Default) {

    fun initiateReasoning(type: CatalogType, graph: String, fdkId: String) {
        val rdfData = ExternalRDFData(
            orgData = referenceDataCache.organizations(),
            losData = referenceDataCache.los(),
            eurovocs = referenceDataCache.eurovocs(),
            dataThemes = referenceDataCache.dataThemes(),
            conceptStatuses = referenceDataCache.conceptStatuses(),
            conceptSubjects = referenceDataCache.conceptSubjects(),
            ianaMediaTypes = referenceDataCache.ianaMediaTypes(),
            fileTypes = referenceDataCache.fileTypes(),
            openLicenses = referenceDataCache.openLicenses(),
            linguisticSystems = referenceDataCache.linguisticSystems(),
            locations = referenceDataCache.locations(),
            accessRights = referenceDataCache.accessRights(),
            frequencies = referenceDataCache.frequencies(),
            provenance = referenceDataCache.provenance(),
            publisherTypes = referenceDataCache.publisherTypes(),
            admsStatuses = referenceDataCache.admsStatuses(),
            roleTypes = referenceDataCache.roleTypes(),
            evidenceTypes = referenceDataCache.evidenceTypes(),
            channelTypes = referenceDataCache.channelTypes(),
            mainActivities = referenceDataCache.mainActivities(),
            weekDays = referenceDataCache.weekDays()
        )
        try {
            when {
                rdfData.orgData.isEmpty -> throw Exception("missing org data")
                rdfData.losData.isEmpty -> throw Exception("missing los data")
                rdfData.eurovocs.isEmpty -> throw Exception("missing eurovocs data")
                rdfData.dataThemes.isEmpty -> throw Exception("missing data themes")
                else -> launchReasoning(type, graph, fdkId, rdfData)
            }
        } catch (ex: Exception) {
            LOGGER.warn("reasoning activity $type was aborted: ${ex.message}")
        }
    }

    private fun launchReasoning(
        type: CatalogType,
        graph: String,
        fdkId: String,
        rdfData: ExternalRDFData
    ) = launch {
        try {
            val reasonedGraph = when (type) {
                CatalogType.CONCEPTS -> conceptService.reasonConcept(graph, fdkId, rdfData)
                else -> ""
            }
        } catch (ex: Exception) {
            LOGGER.warn("reasoning activity $type was aborted: ${ex.message}")
        }
    }
}
