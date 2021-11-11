package no.fdk.fdk_reasoning_service.service

import no.fdk.fdk_reasoning_service.model.CatalogType
import no.fdk.fdk_reasoning_service.model.TurtleDBO
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.Resource
import org.apache.jena.riot.Lang
import org.apache.jena.vocabulary.DCAT
import org.apache.jena.vocabulary.RDF
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.findById
import org.springframework.stereotype.Service

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

    fun reasonHarvestedDatasets() {
        datasetMongoTemplate.findById<TurtleDBO>("catalog-union-graph", "turtle")
            ?.let { parseRDFResponse(ungzip(it.turtle), Lang.TURTLE, "datasets") }
            ?.let { reasoningService.catalogReasoning(it, CatalogType.DATASETS) }
            ?. run { separateAndSaveDatasets() }
            ?: run { LOGGER.error("harvested datasets not found", Exception()) }
    }

    private fun Model.separateAndSaveDatasets() {
        datasetMongoTemplate.save(createUnionDBO(), "fdkCatalogs")

        splitDatasetCatalogsFromRDF()
            .forEach { it.saveCatalogAndDatasetModels() }
        LOGGER.debug("reasoned datasets saved to db")
    }

    private fun CatalogAndDatasets.saveCatalogAndDatasetModels() {
        datasetMongoTemplate.save(catalog.createDBO(fdkId), "fdkCatalogs")

        datasets.forEach { datasetMongoTemplate.save(it.dataset.createDBO(it.fdkId), "fdkDatasets") }
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
                .mapNotNull { dataset -> dataset.resource.extractDataset() }

            var catalogModelWithoutDatasets = listProperties().toModel()
            catalogModelWithoutDatasets.setNsPrefixes(model.nsPrefixMap)

            listProperties().toList()
                .filter { it.isResourceProperty() }
                .forEach {
                    if (it.predicate != DCAT.dataset) {
                        catalogModelWithoutDatasets =
                            catalogModelWithoutDatasets.recursiveAddNonDatasetResource(it.resource, 5)
                    }
                }

            var datasetsUnion = ModelFactory.createDefaultModel()
            catalogDatasets.forEach { datasetsUnion = datasetsUnion.union(it.dataset) }

            CatalogAndDatasets(
                fdkId = fdkIdAndRecordURI.fdkId,
                datasets = catalogDatasets,
                catalog = catalogModelWithoutDatasets
                    .union(datasetsUnion)
                    .union(catalogRecordModel(fdkIdAndRecordURI.recordURI))
            )
        }
    }

    private fun Resource.extractDataset(): DatasetModel? {
        var datasetModel = listProperties().toModel()
        datasetModel = datasetModel.setNsPrefixes(model.nsPrefixMap)

        listProperties().toList()
            .filter { it.isResourceProperty() }
            .forEach {
                datasetModel = datasetModel.recursiveAddNonDatasetResource(it.resource, 10)
            }

        val fdkIdAndRecordURI = extractFDKIdAndRecordURI()

        return if (fdkIdAndRecordURI == null) null
        else DatasetModel(
            fdkId = fdkIdAndRecordURI.fdkId,
            dataset = datasetModel.union(catalogRecordModel(fdkIdAndRecordURI.recordURI))
        )
    }

    private fun Model.recursiveAddNonDatasetResource(resource: Resource, maxDepth: Int): Model {
        val newDepth = maxDepth - 1

        if (resourceShouldBeAdded(resource)) {
            add(resource.listProperties())

            if (newDepth > 0) {
                resource.listProperties().toList()
                    .filter { it.isResourceProperty() }
                    .forEach { recursiveAddNonDatasetResource(it.resource, newDepth) }
            }
        }

        return this
    }

    private fun Model.resourceShouldBeAdded(resource: Resource): Boolean {
        val types = resource.listProperties(RDF.type)
            .toList()
            .map { it.`object` }

        return when {
            types.contains(DCAT.Dataset) -> false
            containsTriple("<${resource.uri}>", "a", "?o") -> false
            else -> true
        }
    }

    private data class CatalogAndDatasets(
        val fdkId: String,
        val catalog: Model,
        val datasets: List<DatasetModel>
    )

    private data class DatasetModel(
        val fdkId: String,
        val dataset: Model
    )

}
