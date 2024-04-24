package no.fdk.fdk_reasoning_service.service

import no.fdk.fdk_reasoning_service.Application
import no.fdk.fdk_reasoning_service.model.CatalogType
import no.fdk.fdk_reasoning_service.model.ExternalRDFData
import no.fdk.fdk_reasoning_service.rdf.BR
import no.fdk.fdk_reasoning_service.rdf.CV
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.Resource
import org.apache.jena.reasoner.rulesys.GenericRuleReasoner
import org.apache.jena.reasoner.rulesys.Rule
import org.apache.jena.sparql.vocabulary.FOAF
import org.apache.jena.vocabulary.DCTerms
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder

private val LOGGER: Logger = LoggerFactory.getLogger(Application::class.java)


fun catalogReasoning(catalogModel: Model, catalogType: CatalogType, rdfData: ExternalRDFData, orgURI: String): Model =
    catalogModel
        .union(
            catalogType.extendedPublishersModel(
                orgData = rdfData.orgData,
                catalogData = catalogModel,
                orgURI = orgURI
            )
        )
        .union(catalogType.deductionsModel(catalogData = catalogModel, rdfData = rdfData))

private fun CatalogType.extendedPublishersModel(orgData: Model, catalogData: Model, orgURI: String): Model {
    val publisherPredicates = when (this) {
        CatalogType.PUBLICSERVICES -> listOf(CV.hasCompetentAuthority, CV.ownedBy)
        else -> listOf(DCTerms.publisher)
    }
    val publisherResources = if (this == CatalogType.DATASETS) {
        catalogData.extractOrganizations(publisherPredicates).plus(catalogData.extreactQualifiedAttributionAgents())
    } else catalogData.extractOrganizations(publisherPredicates)
    return orgData.createModelOfOrganizationsWithOrgData(
        organizationURIs = publisherResources
            .filter { it.dctIdentifierIsInadequate() }
            .filter { it.isURIResource }
            .mapNotNull { it.uri }
            .toSet(),
        orgsURI = orgURI
    ).addOrgPathAndNameWhenMissing(publisherResources.toSet(), catalogData, orgData, orgURI)
}

private fun Model.addOrgPathAndNameWhenMissing(publishers: Set<Resource>, catalogData: Model, orgData: Model, orgBaseURI: String): Model {
    publishers.asSequence()
        .filterNot { containsTriple("<$it>", "<${FOAF.name.uri}>", "?o") }
        .filterNot { it.hasProperty(FOAF.name) }
        .map { Pair(it, catalogData.dctIdentifierIfOrgId(it)?.let { orgId -> orgData.getResource(orgURI(orgId, orgBaseURI)) }) }
        .filter { it.second != null }
        .forEach { it.first.safeAddProperty(FOAF.name, it.second?.getProperty(FOAF.name)?.`object`) }

    val publishersMissingOrgPath = publishers.asSequence()
        .filterNot { containsTriple("<$it>", "<${BR.orgPath.uri}>", "?o") }
        .filterNot { it.hasProperty(BR.orgPath) }

    publishersMissingOrgPath
        .map { Pair(it, catalogData.dctIdentifierIfOrgId(it)?.let { orgId -> orgData.getResource(orgURI(orgId, orgBaseURI)) }) }
        .filter { it.second != null }
        .forEach { it.first.safeAddProperty(BR.orgPath, it.second?.getProperty(BR.orgPath)?.`object`) }

    publishersMissingOrgPath
        .filterNot { containsTriple("<$it>", "<${BR.orgPath.uri}>", "?o") }
        .map { Triple(it, catalogData.dctIdentifierIfOrgId(it), it.foafName()) }
        .forEach { getOrgPath(it.second, it.third, orgBaseURI)?.let { orgPath -> it.first.addProperty(BR.orgPath, orgPath) } }

    return this
}

private fun Model.dctIdentifierIfOrgId(publisher: Resource): String? {
    val orgId: String? = getProperty(publisher, DCTerms.identifier)?.string
    val regex = Regex("""^[0-9]{9}$""")
    val matching = regex.findAll(orgId ?: "").toList()

    return if (matching.size == 1) orgId
    else null
}

private fun Resource.foafName(): String? {
    val names = listProperties(FOAF.name)?.toList()
    val nb = names?.find { it.language == "nb" }
    val nn = names?.find { it.language == "nn" }
    val en = names?.find { it.language == "en" }
    return when {
        names == null -> null
        names.isEmpty() -> null
        names.size == 1 -> names.first().string
        nb != null -> nb.string
        nn != null -> nn.string
        en != null -> en.string
        else -> names.first().string
    }
}

private fun getOrgPath(orgId: String?, orgName: String?, orgBaseURI: String): String? =
    when {
        orgId != null -> orgPathAdapter(orgId, orgBaseURI)
        orgName != null -> orgPathAdapter(orgName, orgBaseURI)
        else -> null
    }

private fun orgURI(orgId: String, orgBaseURI: String) = "$orgBaseURI/$orgId"

private fun orgPathAdapter(value: String, orgBaseURI: String): String? {
    val uri = "$orgBaseURI/orgpath/${URLEncoder.encode(value, "UTF-8")}"
    with(URI(uri).toURL().openConnection() as HttpURLConnection) {
        setRequestProperty("Accept", "text/plain")
        try {
            if (HttpStatus.valueOf(responseCode).is2xxSuccessful) {
                return inputStream.bufferedReader().use(BufferedReader::readText)
            } else {
                LOGGER.error(
                    "Fetch of orgPath for value $value failed, status: $responseCode",
                    Exception("Fetch of orgPath for value $value failed")
                )
            }
        } catch (ex: Exception) {
            LOGGER.error("Error fetching orgPath for value $value", ex)
        } finally {
            disconnect()
        }
        return null
    }
}

private fun CatalogType.deductionsModel(catalogData: Model, rdfData: ExternalRDFData): Model =
    when (this) {
        CatalogType.CONCEPTS -> ModelFactory.createInfModel(
            GenericRuleReasoner(Rule.parseRules(conceptRules)),
            catalogData
        ).deductionsModel

        CatalogType.DATASETS -> catalogData.fdkPrefix().datasetDeductions(rdfData)
        CatalogType.DATASERVICES -> ModelFactory.createInfModel(
            GenericRuleReasoner(Rule.parseRules(dataServiceRules)),
            catalogData
        ).deductionsModel

        CatalogType.INFORMATIONMODELS -> catalogData.informationModelDeductions(rdfData)
        CatalogType.PUBLICSERVICES -> catalogData.servicesDeductions(rdfData)
        else -> ModelFactory.createDefaultModel()
    }

private fun Model.informationModelDeductions(rdfData: ExternalRDFData): Model {
    val deductions = ModelFactory.createInfModel(
        GenericRuleReasoner(Rule.parseRules(infoModelRules)),
        this
    ).deductionsModel

    val themeLabelDeductions = union(deductions).themePrefLabelDeductions(rdfData)

    return deductions.union(themeLabelDeductions)
}

private fun Model.servicesDeductions(rdfData: ExternalRDFData): Model {
    val deductions = ModelFactory.createInfModel(
        GenericRuleReasoner(Rule.parseRules(serviceRules)),
        this
    ).deductionsModel

    val themeLabelDeductions = union(deductions).themePrefLabelDeductions(rdfData)

    return deductions.union(themeLabelDeductions)
}

private fun Model.datasetDeductions(rdfData: ExternalRDFData): Model {
    val deductions = ModelFactory.createInfModel(
        GenericRuleReasoner(Rule.parseRules(datasetRules)).bindSchema(rdfData.losData),
        this
    ).deductionsModel

    val themeLabelDeductions = union(deductions).themePrefLabelDeductions(rdfData)

    return deductions.union(themeLabelDeductions)
}

private fun Model.themePrefLabelDeductions(rdfData: ExternalRDFData): Model {
    // tag themes that's missing prefLabel in input model, to easier add all lang-options for relevant themes
    val themesMissingLabels = ModelFactory.createInfModel(
        GenericRuleReasoner(Rule.parseRules(tagThemesMissingLabel)),
        this
    ).deductionsModel

    // get theme labels as dct:title, so as not to confuse reasoner when adding them as prefLabel to input model later
    val themeTitles = ModelFactory.createInfModel(
        GenericRuleReasoner(Rule.parseRules(labelToTitle)),
        rdfData.losData.union(rdfData.eurovocs).union(rdfData.dataThemes)
    ).deductionsModel

    // add prefLabel from themeTitles for themes tagged as missing label
    return ModelFactory.createInfModel(
        GenericRuleReasoner(Rule.parseRules(addThemeTitles)).bindSchema(themeTitles.union(themesMissingLabels)),
        this
    ).deductionsModel
}

