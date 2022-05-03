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
class EventService(
    private val reasoningService: ReasoningService,
    private val eventMongoTemplate: MongoTemplate
) {

    fun getAllEvents(lang: Lang): String =
        eventMongoTemplate.findById<TurtleDBO>(UNION_ID, "fdkEvent")
            ?.toRDF(lang)
            ?: ModelFactory.createDefaultModel().createRDFResponse(lang)

    fun getEventById(id: String, lang: Lang): String? =
        eventMongoTemplate.findById<TurtleDBO>(id, "fdkEvent")
            ?.toRDF(lang)

    fun reasonReportedChanges(harvestReport: HarvestReport, rdfData: ExternalRDFData, start: Date): ReasoningReport =
        try {
            harvestReport.changedResources
                .forEach { reasonEvent(it.fdkId, rdfData) }

            ReasoningReport(
                id = harvestReport.id,
                url = harvestReport.url,
                dataType = CatalogType.EVENTS.toReportType(),
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
                dataType = CatalogType.EVENTS.toReportType(),
                harvestError = true,
                errorMessage = ex.message,
                startTime = start.formatWithOsloTimeZone(),
                endTime = formatNowWithOsloTimeZone()
            )
        }

    private fun reasonEvent(eventId: String, rdfData: ExternalRDFData) {
        eventMongoTemplate.findById<TurtleDBO>(eventId, "turtle")
            ?.let { parseRDFResponse(ungzip(it.turtle), Lang.TURTLE, "events") }
            ?.let { reasoningService.catalogReasoning(it, CatalogType.EVENTS, rdfData) }
            ?.also { eventMongoTemplate.save(it.createDBO(eventId), "fdkEvent") }
            ?: throw Exception("missing database data, harvest-reasoning was stopped")
    }

    fun updateUnion() {
        var eventUnion = ModelFactory.createDefaultModel()

        eventMongoTemplate.findAll<TurtleDBO>("fdkEvent")
            .filter { it.id != UNION_ID }
            .map { parseRDFResponse(ungzip(it.turtle), Lang.TURTLE, null) }
            .forEach { eventUnion = eventUnion.union(it) }

        eventMongoTemplate.save(eventUnion.createUnionDBO(), "fdkEvent")
    }

}
