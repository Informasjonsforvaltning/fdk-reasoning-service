package no.fdk.fdk_reasoning_service.service

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import no.fdk.fdk_reasoning_service.model.CatalogType
import no.fdk.fdk_reasoning_service.model.ExternalRDFData
import no.fdk.fdk_reasoning_service.model.HarvestReport
import no.fdk.fdk_reasoning_service.model.ReasoningReport
import no.fdk.fdk_reasoning_service.rdf.ModellDCATAPNO
import no.fdk.fdk_reasoning_service.repository.InformationModelRepository
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.RDFNode
import org.apache.jena.rdf.model.Resource
import org.apache.jena.riot.Lang
import org.apache.jena.vocabulary.DCAT
import org.apache.jena.vocabulary.RDF
import org.apache.jena.vocabulary.SKOS
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.Date

private val LOGGER = LoggerFactory.getLogger(InfoModelService::class.java)

@Service
class InfoModelService(
    private val reasoningService: ReasoningService,
    private val repository: InformationModelRepository
) {

    fun getAllInformationModelCatalogs(lang: Lang): String =
        repository.findReasonedUnion()
            ?.let {
                if (lang == Lang.TURTLE) it
                else parseRDFResponse(it, Lang.TURTLE, null)?.createRDFResponse(lang)
            }
            ?: ModelFactory.createDefaultModel().createRDFResponse(lang)

    fun getInformationModelCatalogById(id: String, lang: Lang): String? =
        repository.findCatalog(id)
            ?.let {
                if (lang == Lang.TURTLE) it
                else parseRDFResponse(it, Lang.TURTLE, null)?.createRDFResponse(lang)
            }

    fun getInformationModelById(id: String, lang: Lang): String? =
        repository.findInformationModel(id)
            ?.let {
                if (lang == Lang.TURTLE) it
                else parseRDFResponse(it, Lang.TURTLE, null)?.createRDFResponse(lang)
            }

    fun reasonReportedChanges(harvestReport: HarvestReport, rdfData: ExternalRDFData, start: Date): ReasoningReport =
        try {
            harvestReport.changedCatalogs
                .forEach { reasonCatalogModels(it.fdkId, rdfData, start) }

            ReasoningReport(
                id = harvestReport.id,
                url = harvestReport.url,
                dataType = CatalogType.INFORMATIONMODELS.toReportType(),
                harvestError = false,
                startTime = start.formatWithOsloTimeZone(),
                endTime = formatNowWithOsloTimeZone(),
                changedCatalogs = harvestReport.changedCatalogs,
                changedResources = harvestReport.changedResources,
                removedResources = harvestReport.removedResources
            )
        } catch (ex: Exception) {
            LOGGER.error("information model reasoning failed for ${harvestReport.url}", ex)
            ReasoningReport(
                id = harvestReport.id,
                url = harvestReport.url,
                dataType = CatalogType.INFORMATIONMODELS.toReportType(),
                harvestError = true,
                errorMessage = ex.message,
                startTime = start.formatWithOsloTimeZone(),
                endTime = formatNowWithOsloTimeZone()
            )
        }

    private fun reasonCatalogModels(catalogId: String, rdfData: ExternalRDFData, start: Date) {
        repository.findHarvestedCatalog(catalogId)
            ?.let { parseRDFResponse(it, Lang.TURTLE, "information-models") }
            ?.let { reasoningService.catalogReasoning(it, CatalogType.INFORMATIONMODELS, rdfData) }
            ?.union(rdfData.toModel())
            ?.also { it.separateAndSaveInformationModels() }
            ?: throw Exception("missing database data, harvest-reasoning was stopped")
    }

    private fun ExternalRDFData.toModel(): Model {
        val m = ModelFactory.createDefaultModel()
        m.add(selectedThemeTriples())
        m.add(openLicenses)
        m.add(linguisticSystems)
        return m
    }

    fun updateUnion() {
        val catalogUnion = ModelFactory.createDefaultModel()

        repository.findCatalogs()
            .map { parseRDFResponse(it, Lang.TURTLE, null) }
            .forEach { catalogUnion.add(it) }

        repository.saveReasonedUnion(catalogUnion.createRDFResponse(Lang.TURTLE))
    }

    private fun Model.separateAndSaveInformationModels() {
        splitCatalogsFromRDF()
            .forEach { it.saveCatalogAndInformationModels() }
    }

    private fun CatalogAndInfoModels.saveCatalogAndInformationModels() {
        repository.saveCatalog(catalog.createRDFResponse(Lang.TURTLE), fdkId)
        saveInfoModels()
    }

    private fun Model.splitCatalogsFromRDF(): List<CatalogAndInfoModels> =
        listResourcesWithProperty(RDF.type, DCAT.Catalog)
            .toList()
            .filter { it.hasProperty(ModellDCATAPNO.model) }
            .mapNotNull { catalogResource -> catalogResource.extractCatalog() }

    private fun Resource.extractCatalog(): CatalogAndInfoModels? {
        val fdkIdAndRecordURI = extractFDKIdAndRecordURI()
        return if (fdkIdAndRecordURI == null) null
        else {
            val catalogInfoModels: List<InformationModel> = listProperties(ModellDCATAPNO.model)
                .toList()
                .filter { it.isResourceProperty() }
                .filter { it.resource.hasProperty(RDF.type, ModellDCATAPNO.InformationModel) }
                .filter { model.catalogContainsInfoModel(uri, it.resource.uri) }
                .mapNotNull { infoModel -> infoModel.resource.extractInformationModel() }

            val catalogModelWithoutInfoModels = listProperties().toModel()
            catalogModelWithoutInfoModels.setNsPrefixes(model.nsPrefixMap)

            listProperties().toList()
                .filter { it.isResourceProperty() }
                .forEach {
                    if (it.predicate != ModellDCATAPNO.model) {
                        catalogModelWithoutInfoModels
                            .recursiveAddNonInformationModelResource(it.resource)
                    }
                }

            catalogModelWithoutInfoModels.add(catalogRecordModel(fdkIdAndRecordURI.recordURI))

            val catalogModel = ModelFactory.createDefaultModel()
            catalogInfoModels.forEach { catalogModel.add(it.infoModel) }

            CatalogAndInfoModels(
                fdkId = fdkIdAndRecordURI.fdkId,
                catalogWithoutModels = catalogModelWithoutInfoModels,
                catalog = catalogModel.union(catalogModelWithoutInfoModels),
                models = catalogInfoModels
            )
        }
    }

    private fun Resource.extractInformationModel(): InformationModel? {
        val infoModel = listProperties().toModel()
        infoModel.setNsPrefixes(model.nsPrefixMap)

        listProperties().toList()
            .filter { it.isResourceProperty() }
            .forEach { infoModel.recursiveAddNonInformationModelResource(it.resource) }

        val fdkIdAndRecordURI = extractFDKIdAndRecordURI()

        return if (fdkIdAndRecordURI == null) null
        else InformationModel(
            fdkId = fdkIdAndRecordURI.fdkId,
            infoModel = infoModel.union(catalogRecordModel(fdkIdAndRecordURI.recordURI))
        )
    }

    private fun Model.addCodeElementsAssociatedWithCodeList(resource: Resource): Model {
        resource.model
            .listResourcesWithProperty(RDF.type, ModellDCATAPNO.CodeElement)
            .toList()
            .filter { it.hasProperty(SKOS.inScheme, resource) }
            .forEach { codeElement ->
                add(codeElement.listProperties())

                codeElement.listProperties().toList()
                    .filter { it.isResourceProperty() }
                    .forEach { add(it.resource.listProperties()) }
            }

        return this
    }

    private fun Model.recursiveAddNonInformationModelResource(resource: Resource): Model {
        val types = resource.listProperties(RDF.type)
            .toList()
            .map { it.`object` }

        if (resourceShouldBeAdded(resource, types)) {
            add(resource.listProperties())

            resource.listProperties().toList()
                .filter { it.isResourceProperty() }
                .forEach { recursiveAddNonInformationModelResource(it.resource) }

            if (types.contains(ModellDCATAPNO.CodeList)) addCodeElementsAssociatedWithCodeList(resource)
        }

        return this
    }

    private fun Model.resourceShouldBeAdded(resource: Resource, types: List<RDFNode>): Boolean {
        return when {
            types.contains(ModellDCATAPNO.InformationModel) -> false
            types.contains(DCAT.Catalog) -> false
            types.contains(DCAT.CatalogRecord) -> false
            resource.uri == null -> true
            containsTriple("<${resource.uri}>", "a", "?o") -> false
            else -> true
        }
    }

    private fun Model.catalogContainsInfoModel(catalogURI: String, infoModelURI: String): Boolean =
        containsTriple("<$catalogURI>", "<${ModellDCATAPNO.model.uri}>", "<$infoModelURI>")
                && containsTriple("<$infoModelURI>", "a", "<${ModellDCATAPNO.InformationModel.uri}>")

    private fun CatalogAndInfoModels.saveInfoModels() = runBlocking {
        val activitySemaphore = Semaphore(1)
        models.forEach {
            activitySemaphore.withPermit {
                it.infoModel.union(catalogWithoutModels)
                    .createRDFResponse(Lang.TURTLE)
                    .let { serializedModel -> repository.saveInformationModel(serializedModel, it.fdkId) }
            }
        }
    }
}

private data class CatalogAndInfoModels(
    val fdkId: String,
    val catalog: Model,
    val catalogWithoutModels: Model,
    val models: List<InformationModel>
)

private data class InformationModel(
    val fdkId: String,
    val infoModel: Model
)
