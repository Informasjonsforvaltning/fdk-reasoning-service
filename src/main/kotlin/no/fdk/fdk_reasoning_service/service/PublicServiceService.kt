package no.fdk.fdk_reasoning_service.service

import no.fdk.fdk_reasoning_service.model.CatalogType
import no.fdk.fdk_reasoning_service.model.ExternalRDFData
import no.fdk.fdk_reasoning_service.model.HarvestReport
import no.fdk.fdk_reasoning_service.model.ReasoningReport
import no.fdk.fdk_reasoning_service.model.TurtleDBO
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.riot.Lang
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.findAll
import org.springframework.data.mongodb.core.findById
import org.springframework.stereotype.Service
import java.util.Date

private val LOGGER = LoggerFactory.getLogger(EventService::class.java)

@Service
class PublicServiceService(
    private val reasoningService: ReasoningService,
    private val publicServiceMongoTemplate: MongoTemplate
) {

    fun getAllPublicServices(lang: Lang): String =
        publicServiceMongoTemplate.findById<TurtleDBO>(UNION_ID, "fdkPublicService")
            ?.toRDF(lang)
            ?: ModelFactory.createDefaultModel().createRDFResponse(lang)

    fun getPublicServiceById(id: String, lang: Lang): String? =
        publicServiceMongoTemplate.findById<TurtleDBO>(id, "fdkPublicService")
            ?.toRDF(lang)

    fun reasonReportedChanges(harvestReport: HarvestReport, rdfData: ExternalRDFData, start: Date): ReasoningReport =
        try {
            harvestReport.changedResources
                .forEach { reasonService(it.fdkId, rdfData) }

            ReasoningReport(
                id = harvestReport.id,
                url = harvestReport.url,
                dataType = CatalogType.PUBLICSERVICES.toReportType(),
                harvestError = false,
                startTime = start.formatWithOsloTimeZone(),
                endTime = formatNowWithOsloTimeZone(),
                changedResources = harvestReport.changedResources
            )
        } catch (ex: Exception) {
            LOGGER.error("event reasoning failed for ${harvestReport.url}", ex)
            ReasoningReport(
                id = harvestReport.id,
                url = harvestReport.url,
                dataType = CatalogType.PUBLICSERVICES.toReportType(),
                harvestError = true,
                errorMessage = ex.message,
                startTime = start.formatWithOsloTimeZone(),
                endTime = formatNowWithOsloTimeZone()
            )
        }

    private fun reasonService(serviceId: String, rdfData: ExternalRDFData) {
        publicServiceMongoTemplate.findById<TurtleDBO>(serviceId, "turtle")
            ?.let { parseRDFResponse(ungzip(it.turtle), Lang.TURTLE, "public-services") }
            ?.let { reasoningService.catalogReasoning(it, CatalogType.PUBLICSERVICES, rdfData) }
            ?.also { publicServiceMongoTemplate.save(it.createDBO(serviceId), "fdkPublicService") }
            ?: throw Exception("missing database data, harvest-reasoning was stopped")
    }

    fun updateUnion() {
        var serviceUnion = ModelFactory.createDefaultModel()

        publicServiceMongoTemplate.findAll<TurtleDBO>("fdkPublicService")
            .filter { it.id != UNION_ID }
            .map { parseRDFResponse(ungzip(it.turtle), Lang.TURTLE, null) }
            .forEach { serviceUnion = serviceUnion.union(it) }

        publicServiceMongoTemplate.save(serviceUnion.createUnionDBO(), "fdkPublicService")
    }

}
