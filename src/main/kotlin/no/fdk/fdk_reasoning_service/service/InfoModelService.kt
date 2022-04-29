package no.fdk.fdk_reasoning_service.service

import no.fdk.fdk_reasoning_service.model.CatalogType
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

    fun reasonHarvestedInformationModels() {
        try {
            repository.findHarvestedUnion()
                ?.let { parseRDFResponse(it, Lang.TURTLE, "information-models") }
                ?.let { reasoningService.catalogReasoning(it, CatalogType.INFORMATIONMODELS) }
                ?.run { separateAndSaveInformationModels() }
                ?: run { LOGGER.error("missing some or all rdf-data, reasoning of information models was stopped", Exception()) }
        } catch (ex: Exception) {
            LOGGER.error("reasoning of information models failed", ex)
        }
    }

    private fun Model.separateAndSaveInformationModels() {
        repository.saveReasonedUnion(createRDFResponse(Lang.TURTLE))

        splitCatalogsFromRDF()
            .forEach { it.saveCatalogAndInformationModels() }
        LOGGER.debug("reasoned catalogs and information models saved to db")
    }

    private fun CatalogAndInfoModels.saveCatalogAndInformationModels() {
        repository.saveCatalog(catalog.createRDFResponse(Lang.TURTLE), fdkId)

        models.forEach { repository.saveInformationModel(it.infoModel.createRDFResponse(Lang.TURTLE), it.fdkId) }
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

            var catalogModelWithoutInfoModels = listProperties().toModel()
            catalogModelWithoutInfoModels.setNsPrefixes(model.nsPrefixMap)

            listProperties().toList()
                .filter { it.isResourceProperty() }
                .forEach {
                    if (it.predicate != ModellDCATAPNO.model) {
                        catalogModelWithoutInfoModels =
                            catalogModelWithoutInfoModels.recursiveAddNonInformationModelResource(
                                it.resource,
                                5
                            )
                    }
                }

            var catalogModel = catalogModelWithoutInfoModels
            catalogInfoModels.forEach { catalogModel = catalogModel.union(it.infoModel) }

            CatalogAndInfoModels(
                fdkId = fdkIdAndRecordURI.fdkId,
                catalog = catalogModel.union(catalogRecordModel(fdkIdAndRecordURI.recordURI)),
                models = catalogInfoModels
            )
        }
    }

    private fun Resource.extractInformationModel(): InformationModel? {
        var infoModel = listProperties().toModel()
        infoModel = infoModel.setNsPrefixes(model.nsPrefixMap)

        listProperties().toList()
            .filter { it.isResourceProperty() }
            .forEach { infoModel = infoModel.recursiveAddNonInformationModelResource(it.resource, 10) }

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

    private fun Model.recursiveAddNonInformationModelResource(resource: Resource, maxDepth: Int): Model {
        val newDepth = maxDepth - 1
        val types = resource.listProperties(RDF.type)
            .toList()
            .map { it.`object` }

        if (resourceShouldBeAdded(resource, types)) {
            add(resource.listProperties())

            if (newDepth > 0) {
                resource.listProperties().toList()
                    .filter { it.isResourceProperty() }
                    .forEach { recursiveAddNonInformationModelResource(it.resource, newDepth) }
            }

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
}

private data class CatalogAndInfoModels(
    val fdkId: String,
    val catalog: Model,
    val models: List<InformationModel>
)

private data class InformationModel(
    val fdkId: String,
    val infoModel: Model
)
