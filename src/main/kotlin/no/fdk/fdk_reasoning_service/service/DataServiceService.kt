package no.fdk.fdk_reasoning_service.service

import no.fdk.fdk_reasoning_service.model.CatalogType
import no.fdk.fdk_reasoning_service.model.TurtleDBO
import no.fdk.fdk_reasoning_service.rdf.CPSV
import no.fdk.fdk_reasoning_service.rdf.CV
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

private val LOGGER = LoggerFactory.getLogger(DataServiceService::class.java)

@Service
class DataServiceService(
    private val reasoningService: ReasoningService,
    private val dataServiceMongoTemplate: MongoTemplate
) {

    fun getAllDataServiceCatalogs(lang: Lang): String =
        dataServiceMongoTemplate.findById<TurtleDBO>(UNION_ID, "fdkCatalogs")
            ?.toRDF(lang)
            ?: ModelFactory.createDefaultModel().createRDFResponse(lang)

    fun getDataServiceCatalogById(id: String, lang: Lang): String? =
        dataServiceMongoTemplate.findById<TurtleDBO>(id, "fdkCatalogs")
            ?.toRDF(lang)

    fun getDataServiceById(id: String, lang: Lang): String? =
        dataServiceMongoTemplate.findById<TurtleDBO>(id, "fdkServices")
            ?.toRDF(lang)

    fun reasonHarvestedDataServices() {
        dataServiceMongoTemplate.findById<TurtleDBO>("catalog-union-graph", "turtle")
            ?.let { parseRDFResponse(ungzip(it.turtle), Lang.TURTLE, "dataServices") }
            ?.let { reasoningService.catalogReasoning(it, CatalogType.DATASERVICES) }
            ?. run { separateAndSaveDataServices() }
            ?: run { LOGGER.error("harvested data services not found", Exception()) }
    }

    private fun Model.separateAndSaveDataServices() {
        dataServiceMongoTemplate.save(createUnionDBO(), "fdkCatalogs")

        splitDataServiceCatalogsFromRDF()
            .forEach { it.saveCatalogAndDataServiceModels() }
        LOGGER.debug("reasoned data services saved to db")
    }

    private fun CatalogAndDataServices.saveCatalogAndDataServiceModels() {
        dataServiceMongoTemplate.save(catalog.createDBO(fdkId), "fdkCatalogs")

        services.forEach { dataServiceMongoTemplate.save(it.service.createDBO(it.fdkId), "fdkServices") }
    }

    private fun Model.splitDataServiceCatalogsFromRDF(): List<CatalogAndDataServices> =
        listResourcesWithProperty(RDF.type, DCAT.Catalog)
            .toList()
            .mapNotNull { catalogResource -> catalogResource.extractCatalog() }

    private fun Resource.extractCatalog(): CatalogAndDataServices? {
        val fdkIdAndRecordURI = extractFDKIdAndRecordURI()
        return if (fdkIdAndRecordURI == null) null
        else {
            val catalogServices: List<DataService> = listProperties(DCAT.service)
                .toList()
                .mapNotNull { concept -> concept.resource.extractDataService() }

            var catalogModelWithoutServices = listProperties().toModel()
            catalogModelWithoutServices.setNsPrefixes(model.nsPrefixMap)

            listProperties().toList()
                .filter { it.isResourceProperty() }
                .forEach {
                    if (it.predicate != DCAT.service) {
                        catalogModelWithoutServices =
                            catalogModelWithoutServices.recursiveAddNonDataServiceResource(it.resource, 5)
                    }
                }

            var servicesUnion = ModelFactory.createDefaultModel()
            catalogServices.forEach { servicesUnion = servicesUnion.union(it.service) }

            CatalogAndDataServices(
                fdkId = fdkIdAndRecordURI.fdkId,
                services = catalogServices,
                catalog = catalogModelWithoutServices
                    .union(servicesUnion)
                    .union(catalogRecordModel(fdkIdAndRecordURI.recordURI))
            )
        }
    }

    private fun Resource.extractDataService(): DataService? {
        var serviceModel = listProperties().toModel()
        serviceModel = serviceModel.setNsPrefixes(model.nsPrefixMap)

        listProperties().toList()
            .filter { it.isResourceProperty() }
            .forEach { serviceModel = serviceModel.recursiveAddNonDataServiceResource(it.resource, 10) }

        val fdkIdAndRecordURI = extractFDKIdAndRecordURI()

        return if (fdkIdAndRecordURI == null) null
        else DataService(
            fdkId = fdkIdAndRecordURI.fdkId,
            service = serviceModel.union(catalogRecordModel(fdkIdAndRecordURI.recordURI))
        )
    }

    private fun Model.recursiveAddNonDataServiceResource(resource: Resource, maxDepth: Int): Model {
        val newDepth = maxDepth - 1

        if (resourceShouldBeAdded(resource)) {
            add(resource.listProperties())

            if (newDepth > 0) {
                resource.listProperties().toList()
                    .filter { it.isResourceProperty() }
                    .forEach { recursiveAddNonDataServiceResource(it.resource, newDepth) }
            }
        }

        return this
    }

    private fun Model.resourceShouldBeAdded(resource: Resource): Boolean {
        val types = resource.listProperties(RDF.type)
            .toList()
            .map { it.`object` }

        return when {
            types.contains(DCAT.DataService) -> false
            containsTriple("<${resource.uri}>", "a", "?o") -> false
            else -> true
        }
    }

    private data class CatalogAndDataServices(
        val fdkId: String,
        val catalog: Model,
        val services: List<DataService>
    )

    private data class DataService(
        val fdkId: String,
        val service: Model
    )

}
