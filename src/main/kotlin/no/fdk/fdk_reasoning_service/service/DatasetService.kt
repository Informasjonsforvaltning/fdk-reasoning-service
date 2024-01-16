package no.fdk.fdk_reasoning_service.service

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import no.fdk.fdk_reasoning_service.model.CatalogType
import no.fdk.fdk_reasoning_service.model.ExternalRDFData
import no.fdk.fdk_reasoning_service.model.HarvestReport
import no.fdk.fdk_reasoning_service.model.ReasoningReport
import no.fdk.fdk_reasoning_service.model.TurtleDBO
import no.fdk.fdk_reasoning_service.rdf.DCAT3
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.Resource
import org.apache.jena.riot.Lang
import org.apache.jena.vocabulary.DCAT
import org.apache.jena.vocabulary.RDF
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.findAll
import org.springframework.data.mongodb.core.findById
import org.springframework.stereotype.Service
import java.util.Date

private val LOGGER = LoggerFactory.getLogger(DatasetService::class.java)

@Service
class DatasetService(
    private val reasoningService: ReasoningService,
    private val datasetMongoTemplate: MongoTemplate
) {

    fun getAllDatasetCatalogs(lang: Lang): String =
        datasetMongoTemplate.findById<TurtleDBO>(UNION_ID, "fdkCatalogs")
            ?.toRDF(lang)
            ?: ModelFactory.createDefaultModel().createRDFResponse(lang)

    fun getDatasetCatalogById(id: String, lang: Lang): String? =
        datasetMongoTemplate.findById<TurtleDBO>(id, "fdkCatalogs")
            ?.toRDF(lang)

    fun getDatasetById(id: String, lang: Lang): String? =
        datasetMongoTemplate.findById<TurtleDBO>(id, "fdkDatasets")
            ?.toRDF(lang)

    fun reasonReportedChanges(harvestReport: HarvestReport, rdfData: ExternalRDFData, start: Date): ReasoningReport =
        try {
            harvestReport.changedCatalogs
                .forEach { reasonCatalogDatasets(it.fdkId, rdfData) }

            ReasoningReport(
                id = harvestReport.id,
                url = harvestReport.url,
                dataType = CatalogType.DATASETS.toReportType(),
                harvestError = false,
                startTime = start.formatWithOsloTimeZone(),
                endTime = formatNowWithOsloTimeZone(),
                changedCatalogs = harvestReport.changedCatalogs,
                changedResources = harvestReport.changedResources,
                removedResources = harvestReport.removedResources
            )
        } catch (ex: Exception) {
            LOGGER.error("dataset reasoning failed for ${harvestReport.url}", ex)
            ReasoningReport(
                id = harvestReport.id,
                url = harvestReport.url,
                dataType = CatalogType.DATASETS.toReportType(),
                harvestError = true,
                errorMessage = ex.message,
                startTime = start.formatWithOsloTimeZone(),
                endTime = formatNowWithOsloTimeZone()
            )
        }

    private fun reasonCatalogDatasets(catalogId: String, rdfData: ExternalRDFData) {
        datasetMongoTemplate.findById<TurtleDBO>(harvestedCatalogID(catalogId), "turtle")
            ?.let { parseRDFResponse(ungzip(it.turtle), Lang.TURTLE, "datasets") }
            ?.let { reasoningService.catalogReasoning(it, CatalogType.DATASETS, rdfData) }
            ?.union(rdfData.toModel())
            ?.also { it.separateAndSaveDatasets() }
            ?: throw Exception("missing database data, harvest-reasoning was stopped")
    }

    private fun ExternalRDFData.toModel(): Model {
        val m = ModelFactory.createDefaultModel()
        m.add(selectedThemeTriples())
        m.add(ianaMediaTypes)
        m.add(fileTypes)
        m.add(openLicenses)
        m.add(linguisticSystems)
        m.add(locations)
        m.add(accessRights)
        m.add(frequencies)
        return m
    }

    fun updateUnion() {
        val catalogUnion = ModelFactory.createDefaultModel()

        datasetMongoTemplate.findAll<TurtleDBO>("fdkCatalogs")
            .filter { it.id != UNION_ID }
            .map { parseRDFResponse(ungzip(it.turtle), Lang.TURTLE, null) }
            .forEach { catalogUnion.add(it) }

        datasetMongoTemplate.save(catalogUnion.createUnionDBO(), "fdkCatalogs")
    }

    private fun Model.separateAndSaveDatasets() {
        splitDatasetCatalogsFromRDF()
            .forEach { it.saveCatalogAndDatasetModels() }
    }

    private fun CatalogAndDatasets.saveCatalogAndDatasetModels() {
        datasetMongoTemplate.save(catalog.createDBO(fdkId), "fdkCatalogs")
        saveDatasetModels()
    }

    private fun Model.splitDatasetCatalogsFromRDF(): List<CatalogAndDatasets> =
        listResourcesWithProperty(RDF.type, DCAT.Catalog)
            .toList()
            .mapNotNull { catalogResource -> catalogResource.extractCatalog() }

    private fun Resource.extractCatalog(): CatalogAndDatasets? {
        val fdkIdAndRecordURI = extractFDKIdAndRecordURI()
        return if (fdkIdAndRecordURI == null) null
        else {
            val catalogDatasets: List<DatasetModel> = listProperties(DCAT.dataset)
                .toList()
                .filter { it.isResourceProperty() }
                .map { it.resource }
                .flatMap { it.listDatasetResourcesInSeries() }
                .filter { it.isDataset() }
                .mapNotNull { dataset -> dataset.extractDataset() }

            val catalogModelWithoutDatasets = listProperties().toModel()
            catalogModelWithoutDatasets.setNsPrefixes(model.nsPrefixMap)

            listProperties().toList()
                .filter { it.isResourceProperty() }
                .forEach {
                    if (it.predicate != DCAT.dataset) {
                        catalogModelWithoutDatasets.recursiveAddNonDatasetResource(it.resource)
                    }
                }

            val datasetsUnion = ModelFactory.createDefaultModel()
            catalogDatasets.forEach { datasetsUnion.add(it.dataset) }

            catalogModelWithoutDatasets.add(catalogRecordModel(fdkIdAndRecordURI.recordURI))

            CatalogAndDatasets(
                fdkId = fdkIdAndRecordURI.fdkId,
                datasets = catalogDatasets,
                catalogWithoutDatasets = catalogModelWithoutDatasets,
                catalog = catalogModelWithoutDatasets.union(datasetsUnion)

            )
        }
    }

    private fun Resource.extractDataset(): DatasetModel? {
        val datasetModel = listProperties().toModel()
        datasetModel.setNsPrefixes(model.nsPrefixMap)

        listProperties().toList()
            .filter { it.isResourceProperty() }
            .forEach {
                datasetModel.recursiveAddNonDatasetResource(it.resource)
            }

        val fdkIdAndRecordURI = extractFDKIdAndRecordURI()

        return if (fdkIdAndRecordURI == null) null
        else DatasetModel(
            fdkId = fdkIdAndRecordURI.fdkId,
            dataset = datasetModel.union(catalogRecordModel(fdkIdAndRecordURI.recordURI))
        )
    }

    private fun Model.recursiveAddNonDatasetResource(resource: Resource): Model {
        if (resourceShouldBeAdded(resource)) {
            add(resource.listProperties())

            resource.listProperties().toList()
                .filter { it.isResourceProperty() }
                .forEach { recursiveAddNonDatasetResource(it.resource) }
        }

        return this
    }

    private fun Model.resourceShouldBeAdded(resource: Resource): Boolean {

        return when {
            resource.isDataset() -> false
            containsTriple("<${resource.uri}>", "a", "?o") -> false
            else -> true
        }
    }

    private fun Resource.listDatasetResourcesInSeries(): List<Resource> =
        if (isDatasetSeries()) {
            val datasetsInSeries = model.listResourcesWithProperty(DCAT3.inSeries, this).toList()
            datasetsInSeries.add(this)
            datasetsInSeries
        } else listOf(this)

    private fun Resource.isDataset(): Boolean {
        val types = listProperties(RDF.type)
            .toList()
            .map { it.`object` }

        return types.contains(DCAT.Dataset)
    }

    private fun Resource.isDatasetSeries(): Boolean {
        val types = listProperties(RDF.type)
            .toList()
            .map { it.`object` }

        return types.contains(DCAT3.DatasetSeries)
    }

    private fun harvestedCatalogID(fdkId: String): String =
        "catalog-$fdkId"

    private data class CatalogAndDatasets(
        val fdkId: String,
        val catalog: Model,
        val catalogWithoutDatasets: Model,
        val datasets: List<DatasetModel>
    )

    private data class DatasetModel(
        val fdkId: String,
        val dataset: Model
    )

    private fun CatalogAndDatasets.saveDatasetModels() = runBlocking {
        val activitySemaphore = Semaphore(1)
        datasets.forEach {
            activitySemaphore.withPermit {
                it.dataset.union(catalogWithoutDatasets)
                    .createDBO(it.fdkId)
                    .let { dbo -> datasetMongoTemplate.save(dbo, "fdkDatasets") }
            }
        }
    }

}
